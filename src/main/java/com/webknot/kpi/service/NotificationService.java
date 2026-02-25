package com.webknot.kpi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.models.NotificationEvent;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.repository.NotificationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {
    public static final String TYPE_MANAGER_EMPLOYEE_PAIR_SUBMITTED = "MANAGER_EMPLOYEE_PAIR_SUBMITTED";
    public static final String TYPE_FORGOT_PASSWORD_REQUESTED = "FORGOT_PASSWORD_REQUESTED";
    public static final String TYPE_EMPLOYEE_SUBMITTED_FOR_REVIEW = "EMPLOYEE_SUBMITTED_FOR_REVIEW";

    private static final int DEFAULT_CURSOR_LIMIT = 25;
    private static final int MAX_CURSOR_LIMIT = 100;

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationEventRepository notificationEventRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, CopyOnWriteArrayList<EmitterSubscription>> emittersByRecipient = new ConcurrentHashMap<>();

    public NotificationService(NotificationEventRepository notificationEventRepository,
                               EmployeeRepository employeeRepository,
                               ObjectMapper objectMapper) {
        this.notificationEventRepository = notificationEventRepository;
        this.employeeRepository = employeeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public NotificationPage listForActor(Authentication authentication,
                                         String typesCsv,
                                         Integer limit,
                                         String cursor,
                                         Boolean unreadOnly,
                                         boolean requireAdmin) {
        Employee actor = requireActor(authentication, requireAdmin);
        Set<String> types = normalizeTypes(typesCsv);
        boolean onlyUnread = Boolean.TRUE.equals(unreadOnly);

        int pageSize = normalizeLimit(limit);
        Long startBeforeId = parseCursor(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<NotificationEvent> rows = fetchRows(actor.getEmployeeId(), types, onlyUnread, startBeforeId, pageable);
        boolean hasMore = rows.size() > pageSize;
        List<NotificationEvent> items = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasMore && !items.isEmpty()
                ? String.valueOf(items.get(items.size() - 1).getId())
                : null;

        long unreadCount = types.isEmpty()
                ? notificationEventRepository.countByRecipient_EmployeeIdAndReadFalse(actor.getEmployeeId())
                : notificationEventRepository.countByRecipient_EmployeeIdAndReadFalseAndTypeIn(actor.getEmployeeId(), types);

        List<NotificationPayload> payloadItems = items.stream()
                .map(this::toPayload)
                .toList();
        return new NotificationPage(payloadItems, nextCursor, unreadCount);
    }

    @Transactional
    public NotificationPayload markRead(Authentication authentication, Long id, boolean requireAdmin) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid notification id.");
        }
        Employee actor = requireActor(authentication, requireAdmin);

        NotificationEvent row = notificationEventRepository.findByIdAndRecipient_EmployeeId(id, actor.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));

        if (!row.isRead()) {
            LocalDateTime now = LocalDateTime.now();
            notificationEventRepository.markRead(id, actor.getEmployeeId(), now);
            row.setRead(true);
            row.setReadAt(now);
            row.setUpdatedAt(now);
        }
        return toPayload(row);
    }

    @Transactional
    public int markAllRead(Authentication authentication, boolean requireAdmin) {
        Employee actor = requireActor(authentication, requireAdmin);
        return notificationEventRepository.markAllRead(actor.getEmployeeId(), LocalDateTime.now());
    }

    public SseEmitter subscribe(Authentication authentication, String typesCsv, boolean requireAdmin) {
        Employee actor = requireActor(authentication, requireAdmin);
        Set<String> types = normalizeTypes(typesCsv);

        SseEmitter emitter = new SseEmitter(0L);
        EmitterSubscription subscription = new EmitterSubscription(emitter, types);
        String recipientEmployeeId = actor.getEmployeeId();
        emittersByRecipient
                .computeIfAbsent(recipientEmployeeId, key -> new CopyOnWriteArrayList<>())
                .add(subscription);

        emitter.onCompletion(() -> removeSubscription(recipientEmployeeId, subscription));
        emitter.onTimeout(() -> removeSubscription(recipientEmployeeId, subscription));
        emitter.onError((ex) -> removeSubscription(recipientEmployeeId, subscription));

        try {
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(Map.of("status", "connected", "timestamp", Instant.now().toString())));
        } catch (IOException e) {
            removeSubscription(recipientEmployeeId, subscription);
        }
        return emitter;
    }

    @Transactional
    public void notifyForgotPasswordRequested(Employee targetEmployee,
                                              String requestId,
                                              String adminCode,
                                              Instant expiresAt,
                                              List<Employee> admins) {
        if (targetEmployee == null || admins == null || admins.isEmpty()) return;
        for (Employee admin : admins) {
            if (admin == null || admin.getEmployeeId() == null || admin.getEmployeeId().isBlank()) continue;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("requestId", requestId);
            payload.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
            payload.put("employeeId", targetEmployee.getEmployeeId());
            payload.put("employeeName", targetEmployee.getEmployeeName());
            payload.put("email", targetEmployee.getEmail());
            payload.put("adminCode", adminCode);
            payload.put("recipientRole", "ADMIN");

            String title = "Forgot password request";
            String message = (targetEmployee.getEmail() == null || targetEmployee.getEmail().isBlank())
                    ? "A user requested password reset approval. Admin verification code: " + adminCode
                    : targetEmployee.getEmail() + " requested password reset approval. Admin verification code: " + adminCode;

            createAndDispatch(admin, TYPE_FORGOT_PASSWORD_REQUESTED, title, message, payload);
        }
    }

    @Transactional
    public void notifyEmployeeSubmittedToManager(Employee employee, String month, Long submissionId) {
        if (employee == null) return;
        Employee manager = employee.getManager();
        if (manager == null || manager.getEmployeeId() == null || manager.getEmployeeId().isBlank()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", employee.getEmployeeId());
        payload.put("employeeName", employee.getEmployeeName());
        payload.put("managerId", manager.getEmployeeId());
        payload.put("managerName", manager.getEmployeeName());
        payload.put("month", month);
        payload.put("submissionId", submissionId != null ? String.valueOf(submissionId) : null);
        payload.put("recipientRole", "MANAGER");

        String employeeLabel = firstNonBlank(employee.getEmployeeName(), employee.getEmployeeId(), "An employee");
        String monthLabel = firstNonBlank(month, "current month");
        String title = "Employee submission received";
        String message = employeeLabel + " submitted self review for " + monthLabel + ".";

        createAndDispatch(manager, TYPE_EMPLOYEE_SUBMITTED_FOR_REVIEW, title, message, payload);
    }

    @Transactional
    public void notifyManagerEmployeePairSubmittedToAdmins(Employee employee,
                                                           Employee manager,
                                                           String month,
                                                           Long submissionId) {
        if (employee == null || manager == null) return;
        List<Employee> admins = employeeRepository.findByEmpRole(EmployeeRole.Admin);
        if (admins == null || admins.isEmpty()) return;

        for (Employee admin : admins) {
            if (admin == null || admin.getEmployeeId() == null || admin.getEmployeeId().isBlank()) continue;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("employeeId", employee.getEmployeeId());
            payload.put("employeeName", employee.getEmployeeName());
            payload.put("managerId", manager.getEmployeeId());
            payload.put("managerName", manager.getEmployeeName());
            payload.put("month", month);
            payload.put("submissionId", submissionId != null ? String.valueOf(submissionId) : null);
            payload.put("recipientRole", "ADMIN");

            String employeeLabel = firstNonBlank(employee.getEmployeeName(), employee.getEmployeeId(), "Employee");
            String managerLabel = firstNonBlank(manager.getEmployeeName(), manager.getEmployeeId(), "Manager");
            String monthLabel = firstNonBlank(month, "current month");
            String title = "Manager + Employee submissions completed";
            String message = employeeLabel + " and " + managerLabel + " completed submissions for " + monthLabel + ".";

            createAndDispatch(admin, TYPE_MANAGER_EMPLOYEE_PAIR_SUBMITTED, title, message, payload);
        }
    }

    private List<NotificationEvent> fetchRows(String recipientEmployeeId,
                                              Set<String> types,
                                              boolean onlyUnread,
                                              Long startBeforeId,
                                              Pageable pageable) {
        boolean filterTypes = !types.isEmpty();
        boolean hasCursor = startBeforeId != null;

        if (onlyUnread) {
            if (filterTypes) {
                if (hasCursor) {
                    return notificationEventRepository
                            .findByRecipient_EmployeeIdAndReadFalseAndTypeInAndIdLessThanOrderByIdDesc(
                                    recipientEmployeeId, types, startBeforeId, pageable
                            );
                }
                return notificationEventRepository
                        .findByRecipient_EmployeeIdAndReadFalseAndTypeInOrderByIdDesc(
                                recipientEmployeeId, types, pageable
                        );
            }
            if (hasCursor) {
                return notificationEventRepository
                        .findByRecipient_EmployeeIdAndReadFalseAndIdLessThanOrderByIdDesc(
                                recipientEmployeeId, startBeforeId, pageable
                        );
            }
            return notificationEventRepository.findByRecipient_EmployeeIdAndReadFalseOrderByIdDesc(
                    recipientEmployeeId, pageable
            );
        }

        if (filterTypes) {
            if (hasCursor) {
                return notificationEventRepository.findByRecipient_EmployeeIdAndTypeInAndIdLessThanOrderByIdDesc(
                        recipientEmployeeId, types, startBeforeId, pageable
                );
            }
            return notificationEventRepository.findByRecipient_EmployeeIdAndTypeInOrderByIdDesc(
                    recipientEmployeeId, types, pageable
            );
        }

        if (hasCursor) {
            return notificationEventRepository.findByRecipient_EmployeeIdAndIdLessThanOrderByIdDesc(
                    recipientEmployeeId, startBeforeId, pageable
            );
        }
        return notificationEventRepository.findByRecipient_EmployeeIdOrderByIdDesc(
                recipientEmployeeId, pageable
        );
    }

    private NotificationEvent createAndDispatch(Employee recipient,
                                                String type,
                                                String title,
                                                String message,
                                                Map<String, Object> payload) {
        if (recipient == null || recipient.getEmployeeId() == null || recipient.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("Valid recipient is required");
        }
        String normalizedType = normalizeType(type);
        if (normalizedType.isBlank()) {
            throw new IllegalArgumentException("Unsupported notification type: " + type);
        }

        NotificationEvent row = new NotificationEvent();
        row.setRecipient(recipient);
        row.setType(normalizedType);
        row.setTitle(firstNonBlank(title, normalizedType));
        row.setMessage(firstNonBlank(message, ""));
        row.setPayloadJson(serializePayload(payload));
        row.setRead(false);
        row.setReadAt(null);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());

        NotificationEvent saved = notificationEventRepository.save(row);
        dispatch(saved);
        return saved;
    }

    @Async("taskExecutor")
    private void dispatch(NotificationEvent event) {
        if (event == null || event.getRecipient() == null || event.getRecipient().getEmployeeId() == null) {
            return;
        }
        String recipientEmployeeId = event.getRecipient().getEmployeeId();
        List<EmitterSubscription> subscriptions = emittersByRecipient.get(recipientEmployeeId);
        if (subscriptions == null || subscriptions.isEmpty()) return;

        NotificationPayload payload = toPayload(event);
        String eventName = event.getRecipient().getEmpRole() == EmployeeRole.Admin
                ? "admin-notification"
                : "notification";
        String eventId = event.getId() != null ? String.valueOf(event.getId()) : UUID.randomUUID().toString();

        for (EmitterSubscription subscription : subscriptions) {
            if (!subscription.allows(payload.type())) continue;
            try {
                subscription.emitter().send(SseEmitter.event()
                        .name(eventName)
                        .id(eventId)
                        .data(payload));
            } catch (Exception sendError) {
                removeSubscription(recipientEmployeeId, subscription);
            }
        }
    }

    private void removeSubscription(String recipientEmployeeId, EmitterSubscription subscription) {
        List<EmitterSubscription> subscriptions = emittersByRecipient.get(recipientEmployeeId);
        if (subscriptions == null) return;
        subscriptions.remove(subscription);
        if (subscriptions.isEmpty()) {
            emittersByRecipient.remove(recipientEmployeeId);
        }
    }

    private NotificationPayload toPayload(NotificationEvent row) {
        Map<String, Object> payload = parsePayload(row.getPayloadJson());
        String createdAt = row.getCreatedAt() != null ? row.getCreatedAt().toString() : Instant.now().toString();
        return new NotificationPayload(
                row.getId() != null ? String.valueOf(row.getId()) : UUID.randomUUID().toString(),
                normalizeType(row.getType()),
                firstNonBlank(row.getTitle(), normalizeType(row.getType())),
                firstNonBlank(row.getMessage(), ""),
                createdAt,
                row.isRead(),
                payload
        );
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception e) {
            log.warn("Failed to serialize notification payload: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse notification payload: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private Employee requireActor(Authentication authentication, boolean requireAdmin) {
        String email = authentication == null ? null : firstNonBlank(authentication.getName());
        if (email == null) throw new AccessDeniedException("Unauthorized");
        Employee actor = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unauthorized"));

        if (requireAdmin && actor.getEmpRole() != EmployeeRole.Admin) {
            throw new AccessDeniedException("Admin access required");
        }
        return actor;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_CURSOR_LIMIT;
        return Math.min(limit, MAX_CURSOR_LIMIT);
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            long parsed = Long.parseLong(cursor.trim());
            if (parsed <= 0) throw new IllegalArgumentException("Invalid cursor");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private Set<String> normalizeTypes(String typesCsv) {
        Set<String> out = new LinkedHashSet<>();
        if (typesCsv == null || typesCsv.isBlank()) {
            return out;
        }
        for (String raw : typesCsv.split(",")) {
            String normalized = normalizeType(raw);
            if (!normalized.isBlank()) out.add(normalized);
        }
        return out;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        return switch (normalized) {
            case TYPE_MANAGER_EMPLOYEE_PAIR_SUBMITTED -> TYPE_MANAGER_EMPLOYEE_PAIR_SUBMITTED;
            case TYPE_FORGOT_PASSWORD_REQUESTED -> TYPE_FORGOT_PASSWORD_REQUESTED;
            case TYPE_EMPLOYEE_SUBMITTED_FOR_REVIEW -> TYPE_EMPLOYEE_SUBMITTED_FOR_REVIEW;
            default -> "";
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value == null) continue;
            String text = value.trim();
            if (!text.isBlank()) return text;
        }
        return null;
    }

    public record NotificationPayload(
            String id,
            String type,
            String title,
            String message,
            String createdAt,
            boolean read,
            Map<String, Object> payload
    ) {}

    public record NotificationPage(
            List<NotificationPayload> items,
            String nextCursor,
            long unreadCount
    ) {}

    private record EmitterSubscription(SseEmitter emitter, Set<String> types) {
        boolean allows(String type) {
            if (types == null || types.isEmpty()) return true;
            return types.contains(type);
        }
    }
}
