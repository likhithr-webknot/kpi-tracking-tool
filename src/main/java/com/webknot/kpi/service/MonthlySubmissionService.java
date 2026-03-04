package com.webknot.kpi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.models.MonthlySubmission;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.repository.MonthlySubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class MonthlySubmissionService {
    private static final Logger log = LoggerFactory.getLogger(MonthlySubmissionService.class);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String REVIEW_STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";
    private static final String REVIEW_STATUS_MANAGER_APPROVED = "MANAGER_APPROVED";
    private static final String REVIEW_STATUS_ADMIN_APPROVED = "ADMIN_APPROVED";
    private static final String TYPE_EMPLOYEE = "EMPLOYEE_MONTHLY_SUBMISSION";
    private static final String TYPE_MANAGER_SELF = "MANAGER_SELF_REVIEW";
    private static final int DEFAULT_CURSOR_LIMIT = 20;
    private static final int MAX_CURSOR_LIMIT = 100;

    private final MonthlySubmissionRepository monthlySubmissionRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public MonthlySubmissionService(MonthlySubmissionRepository monthlySubmissionRepository,
                                    EmployeeRepository employeeRepository,
                                    ObjectMapper objectMapper,
                                    NotificationService notificationService) {
        this.monthlySubmissionRepository = monthlySubmissionRepository;
        this.employeeRepository = employeeRepository;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @Transactional(timeout = 30)
    public Map<String, Object> saveDraft(Authentication authentication, Map<String, Object> body) {
        Employee actor = requireActor(authentication);
        Map<String, Object> payload = toMutableMap(body);

        String month = resolveMonth(payload, null);
        String subjectEmployeeId = resolveSubjectEmployeeId(payload, actor);
        Employee subject = requireEmployeeById(subjectEmployeeId);
        String submissionType = resolveSubmissionType(payload, actor, subjectEmployeeId);

        validatePayload(payload, false);
        applyStandardPayloadFields(payload, month, submissionType, subjectEmployeeId);

        MonthlySubmission submission = monthlySubmissionRepository
                .findByEmployee_EmployeeIdAndMonthAndSubmissionType(subjectEmployeeId, month, submissionType)
                .orElseGet(MonthlySubmission::new);

        boolean hasManagerReview = hasNested(payload, "managerReview");
        boolean hasAdminReview = hasNested(payload, "adminReview");
        boolean hasWorkflowReview = hasManagerReview || hasAdminReview;

        submission.setEmployee(subject);
        submission.setMonth(month);
        submission.setSubmissionType(submissionType);
        String reviewStatus = resolveReviewStatus(payload, submission, hasWorkflowReview);
        boolean rejected = REVIEW_STATUS_NEEDS_REVIEW.equalsIgnoreCase(reviewStatus);
        if (hasWorkflowReview) {
            submission.setStatus(rejected ? STATUS_DRAFT : STATUS_SUBMITTED);
        } else {
            submission.setStatus(STATUS_DRAFT);
        }
        submission.setReviewStatus(reviewStatus);

        LocalDateTime now = LocalDateTime.now();
        if (hasManagerReview) {
            submission.setManagerSubmittedAt(now);
            payload.put("managerSubmittedAt", now.toString());
        }
        if (hasAdminReview) {
            submission.setAdminSubmittedAt(now);
            payload.put("adminSubmittedAt", now.toString());
        }
        if (hasWorkflowReview) {
            payload.put("reviewStatus", reviewStatus);
            payload.put("reopenedForResubmission", rejected);
        }

        submission.setPayloadJson(toJson(payload));
        submission.setManagerReviewJson(extractNestedJson(payload, "managerReview", submission.getManagerReviewJson()));
        submission.setAdminReviewJson(extractNestedJson(payload, "adminReview", submission.getAdminReviewJson()));

        MonthlySubmission saved = monthlySubmissionRepository.save(submission);
        log.info("Monthly draft saved: id={}, employee={}, month={}, type={}",
                saved.getId(), subjectEmployeeId, month, submissionType);
        return toResponse(saved, true);
    }

    @Transactional(timeout = 30)
    public Map<String, Object> submit(Authentication authentication, Map<String, Object> body) {
        Employee actor = requireActor(authentication);
        Map<String, Object> payload = toMutableMap(body);

        String month = resolveMonth(payload, null);
        String subjectEmployeeId = resolveSubjectEmployeeId(payload, actor);
        Employee subject = requireEmployeeById(subjectEmployeeId);
        String submissionType = resolveSubmissionType(payload, actor, subjectEmployeeId);

        validatePayload(payload, true);
        applyStandardPayloadFields(payload, month, submissionType, subjectEmployeeId);

        MonthlySubmission submission = monthlySubmissionRepository
                .findByEmployee_EmployeeIdAndMonthAndSubmissionType(subjectEmployeeId, month, submissionType)
                .orElseGet(MonthlySubmission::new);

        submission.setEmployee(subject);
        submission.setMonth(month);
        submission.setSubmissionType(submissionType);
        submission.setPayloadJson(toJson(payload));

        String reviewStatus = resolveReviewStatus(payload, submission, true);
        boolean rejected = REVIEW_STATUS_NEEDS_REVIEW.equalsIgnoreCase(reviewStatus);
        submission.setStatus(rejected ? STATUS_DRAFT : STATUS_SUBMITTED);
        submission.setReviewStatus(reviewStatus);

        LocalDateTime now = LocalDateTime.now();
        if (!rejected) {
            submission.setSubmittedAt(now);
            payload.put("submittedAt", now.toString());
        }
        if (hasNested(payload, "managerReview")) {
            submission.setManagerSubmittedAt(now);
            payload.put("managerSubmittedAt", now.toString());
        }
        if (hasNested(payload, "adminReview")) {
            submission.setAdminSubmittedAt(now);
            payload.put("adminSubmittedAt", now.toString());
        }
        payload.put("reviewStatus", reviewStatus);
        payload.put("reopenedForResubmission", rejected);

        submission.setPayloadJson(toJson(payload));
        submission.setManagerReviewJson(extractNestedJson(payload, "managerReview", submission.getManagerReviewJson()));
        submission.setAdminReviewJson(extractNestedJson(payload, "adminReview", submission.getAdminReviewJson()));

        MonthlySubmission saved = monthlySubmissionRepository.save(submission);
        log.info("Monthly submission saved: id={}, employee={}, month={}, type={}, status={}, reviewStatus={}",
                saved.getId(), subjectEmployeeId, month, submissionType, saved.getStatus(), saved.getReviewStatus());

        triggerNotificationsForSubmission(actor, subject, submissionType, payload, saved, month, rejected);
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> getMine(Authentication authentication, Map<String, String> query) {
        Employee actor = requireActor(authentication);
        String month = resolveMonth(null, query == null ? null : query.get("month"));

        List<MonthlySubmission> candidates;
        if (month != null) {
            candidates = monthlySubmissionRepository.findByEmployee_EmployeeIdAndMonthOrderByUpdatedAtDesc(
                    actor.getEmployeeId(),
                    month
            );
        } else {
            candidates = monthlySubmissionRepository.findByEmployee_EmployeeIdOrderByUpdatedAtDesc(actor.getEmployeeId());
        }
        if (candidates.isEmpty()) return null;

        MonthlySubmission picked = pickSelfSubmission(candidates, actor);
        return toResponse(picked, false);
    }

    @Transactional(readOnly = true, timeout = 10)
    public List<Map<String, Object>> getMyHistory(Authentication authentication) {
        Employee actor = requireActor(authentication);
        List<MonthlySubmission> rows = monthlySubmissionRepository.findByEmployee_EmployeeIdOrderByUpdatedAtDesc(
                actor.getEmployeeId()
        );
        return rows.stream().map(row -> toResponse(row, false)).toList();
    }

    @Transactional(readOnly = true, timeout = 15)
    public Map<String, Object> getCycleHistory(Authentication authentication, Map<String, String> query) {
        Employee actor = requireActor(authentication);
        String employeeFilter = firstNonBlank(
                query == null ? null : query.get("employeeId"),
                query == null ? null : query.get("subjectEmployeeId")
        );
        String monthFrom = resolveMonth(null, query == null ? null : query.get("monthFrom"));
        String monthTo = resolveMonth(null, query == null ? null : query.get("monthTo"));
        if (monthFrom != null && monthTo != null && monthFrom.compareTo(monthTo) > 0) {
            throw new IllegalArgumentException("monthFrom must be <= monthTo.");
        }
        int maxCyclesPerEmployee = parseMaxCyclesPerEmployee(query == null ? null : query.get("maxCyclesPerEmployee"));
        boolean includeManagerSelf = parseBoolean(query == null ? null : query.get("includeManagerSelf"), false);

        Set<String> allowedEmployeeIds = resolveAccessibleEmployeeIds(actor, employeeFilter);
        List<MonthlySubmission> rows = monthlySubmissionRepository.findAllByOrderByUpdatedAtDesc();

        Map<String, Map<String, Object>> employeeEntries = new LinkedHashMap<>();
        Map<String, Set<String>> seenCycleKeys = new HashMap<>();

        for (MonthlySubmission row : rows) {
            if (row == null || row.getEmployee() == null || row.getEmployee().getEmployeeId() == null) {
                continue;
            }
            String employeeId = row.getEmployee().getEmployeeId();
            if (!allowedEmployeeIds.contains(employeeId)) {
                continue;
            }
            if (!includeManagerSelf && TYPE_MANAGER_SELF.equalsIgnoreCase(row.getSubmissionType())) {
                continue;
            }
            if (!withinMonthRange(row.getMonth(), monthFrom, monthTo)) {
                continue;
            }

            Map<String, Object> bucket = employeeEntries.computeIfAbsent(employeeId, key -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("employee", toEmployeeSummary(row.getEmployee()));
                entry.put("cycles", new ArrayList<Map<String, Object>>());
                return entry;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cycles = (List<Map<String, Object>>) bucket.get("cycles");
            Set<String> seenForEmployee = seenCycleKeys.computeIfAbsent(employeeId, key -> new LinkedHashSet<>());
            String cycleKey = row.getMonth() + "|" + normalizeUpper(row.getSubmissionType());
            if (!seenForEmployee.add(cycleKey)) {
                continue;
            }
            if (cycles.size() >= maxCyclesPerEmployee) {
                continue;
            }

            Map<String, Object> full = toResponse(row, true);
            Map<String, Object> cycle = new LinkedHashMap<>();
            cycle.put("id", full.get("id"));
            cycle.put("month", full.get("month"));
            cycle.put("submissionType", full.get("submissionType"));
            cycle.put("status", full.get("status"));
            cycle.put("reviewStatus", full.get("reviewStatus"));
            cycle.put("payload", full.get("payload"));
            cycle.put("managerReview", full.get("managerReview"));
            cycle.put("adminReview", full.get("adminReview"));
            cycle.put("submittedAt", full.get("submittedAt"));
            cycle.put("managerSubmittedAt", full.get("managerSubmittedAt"));
            cycle.put("adminSubmittedAt", full.get("adminSubmittedAt"));
            cycle.put("updatedAt", full.get("updatedAt"));
            cycles.add(cycle);
        }

        List<Map<String, Object>> employees = new ArrayList<>(employeeEntries.values());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeCount", employees.size());
        response.put("monthFrom", monthFrom);
        response.put("monthTo", monthTo);
        response.put("includeManagerSelf", includeManagerSelf);
        response.put("maxCyclesPerEmployee", maxCyclesPerEmployee);
        response.put("employees", employees);
        return response;
    }

    @Transactional(readOnly = true, timeout = 10)
    public Object getManagerTeam(Authentication authentication, Map<String, String> query) {
        Employee actor = requireActor(authentication);
        requireManagerOrAdmin(actor);

        String month = resolveMonth(null, query == null ? null : query.get("month"));
        if (month == null) month = YearMonth.now().toString();
        YearMonth selectedMonth = YearMonth.parse(month);
        String statusFilter = normalizeUpper(query == null ? null : query.get("status"));
        String limitRaw = query == null ? null : query.get("limit");
        String cursorRaw = query == null ? null : query.get("cursor");
        boolean paginationRequested =
                (limitRaw != null && !limitRaw.isBlank()) ||
                (cursorRaw != null && !cursorRaw.isBlank());

        List<Employee> reportees = employeeRepository.findByManager_EmployeeId(actor.getEmployeeId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Employee reportee : reportees) {
            if (!isEligibleForMonth(reportee, selectedMonth)) {
                continue;
            }
            Optional<MonthlySubmission> found = monthlySubmissionRepository
                    .findByEmployee_EmployeeIdAndMonthAndSubmissionType(reportee.getEmployeeId(), month, TYPE_EMPLOYEE);

            Map<String, Object> row;
            if (found.isPresent()) {
                row = toResponse(found.get(), true);
            } else {
                row = buildPendingRow(reportee, month);
            }

            String rowStatus = normalizeUpper(row.get("status"));
            if (statusFilter != null && !statusFilter.equals(rowStatus)) continue;
            out.add(row);
        }

        out.sort((a, b) -> String.valueOf(b.getOrDefault("updatedAt", ""))
                .compareTo(String.valueOf(a.getOrDefault("updatedAt", ""))));
        if (!paginationRequested) {
            return out;
        }

        int pageSize = parseCursorLimit(limitRaw);
        int offset = parseCursorOffset(cursorRaw);
        int safeOffset = Math.min(Math.max(offset, 0), out.size());
        int end = Math.min(safeOffset + pageSize, out.size());
        List<Map<String, Object>> items = new ArrayList<>(out.subList(safeOffset, end));
        String nextCursor = end < out.size() ? String.valueOf(end) : null;
        long submittedCount = out.stream().filter(row -> isSubmittedStatusValue(row.get("status"))).count();
        long reviewedCount = out.stream().filter(this::hasManagerReview).count();
        long pendingManagerReviewCount = out.stream()
                .filter(row -> isSubmittedStatusValue(row.get("status")))
                .filter(row -> !hasManagerReview(row))
                .count();

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("items", items);
        page.put("nextCursor", nextCursor);
        page.put("total", out.size());
        page.put("submittedCount", submittedCount);
        page.put("reviewedCount", reviewedCount);
        page.put("pendingManagerReviewCount", pendingManagerReviewCount);
        return page;
    }

    @Transactional(readOnly = true, timeout = 15)
    public List<Map<String, Object>> getAdminAll(Authentication authentication, Map<String, String> query) {
        Employee actor = requireActor(authentication);
        requireAdmin(actor);

        String month = resolveMonth(null, query == null ? null : query.get("month"));
        String status = normalizeUpper(query == null ? null : query.get("status"));

        List<MonthlySubmission> rows;
        if (month != null && status != null) {
            rows = monthlySubmissionRepository.findByMonthAndStatusOrderByUpdatedAtDesc(month, status);
        } else if (month != null) {
            rows = monthlySubmissionRepository.findByMonthOrderByUpdatedAtDesc(month);
        } else if (status != null) {
            rows = monthlySubmissionRepository.findByStatusOrderByUpdatedAtDesc(status);
        } else {
            rows = monthlySubmissionRepository.findAllByOrderByUpdatedAtDesc();
        }
        return rows.stream().map(row -> toResponse(row, true)).toList();
    }

    @Transactional(readOnly = true, timeout = 10)
    public Map<String, Object> getAdminById(Authentication authentication, Long id) {
        Employee actor = requireActor(authentication);
        requireAdmin(actor);
        MonthlySubmission row = monthlySubmissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
        return toResponse(row, true);
    }

    @Transactional(timeout = 30)
    public Map<String, Object> rejectAdminSubmission(Authentication authentication, Long id, Map<String, Object> body) {
        Employee actor = requireActor(authentication);
        requireAdmin(actor);
        
        MonthlySubmission submission = monthlySubmissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
        
        // Extract and validate rejection comments
        String rejectionComments = body != null 
                ? String.valueOf(body.getOrDefault("rejectionComments", "")).trim() 
                : "";
        
        if (rejectionComments.isEmpty() || rejectionComments.length() < 10) {
            throw new IllegalArgumentException("Rejection comments must be at least 10 characters long.");
        }
        
        // Get the existing payload
        Map<String, Object> payload = parseJsonAsMap(submission.getPayloadJson());
        
        // Create admin review object with rejection
        Map<String, Object> adminReview = new LinkedHashMap<>();
        adminReview.put("action", "REJECT");
        adminReview.put("comments", rejectionComments);
        adminReview.put("rejectedAt", LocalDateTime.now().toString());
        adminReview.put("rejectedBy", actor.getEmployeeId());
        
        payload.put("adminReview", adminReview);
        payload.put("reviewStatus", REVIEW_STATUS_NEEDS_REVIEW);
        payload.put("reopenedForResubmission", true);
        
        // Update submission
        submission.setStatus(STATUS_DRAFT);
        submission.setReviewStatus(REVIEW_STATUS_NEEDS_REVIEW);
        submission.setPayloadJson(toJson(payload));
        submission.setAdminReviewJson(toJson(adminReview));
        submission.setAdminSubmittedAt(LocalDateTime.now());
        
        MonthlySubmission saved = monthlySubmissionRepository.save(submission);
        log.info("Admin rejection submitted: id={}, employee={}, month={}, rejectedBy={}",
                saved.getId(), submission.getEmployee().getEmployeeId(), submission.getMonth(), actor.getEmployeeId());
        
        return toResponse(saved, true);
    }

    @Transactional(timeout = 30)
    public Map<String, Object> deleteAdminById(Authentication authentication, Long id) {
        Employee actor = requireActor(authentication);
        requireAdmin(actor);
        if (!monthlySubmissionRepository.existsById(id)) {
            throw new IllegalArgumentException("Submission not found: " + id);
        }
        monthlySubmissionRepository.deleteById(id);
        log.info("Monthly submission deleted: id={}, by={}", id, actor.getEmployeeId());
        return Map.of("status", "ok", "id", String.valueOf(id));
    }

    @Transactional(timeout = 30)
    public Map<String, Object> submitAdminReview(Authentication authentication, Map<String, Object> body) {
        Employee actor = requireActor(authentication);
        requireAdmin(actor);
        Map<String, Object> payload = toMutableMap(body);

        String month = resolveMonth(payload, null);
        String subjectEmployeeId = resolveSubjectEmployeeId(payload, actor);
        String submissionType = resolveSubmissionType(payload, actor, subjectEmployeeId);

        validatePayload(payload, true);
        applyStandardPayloadFields(payload, month, submissionType, subjectEmployeeId);

        MonthlySubmission existing = monthlySubmissionRepository
                .findByEmployee_EmployeeIdAndMonthAndSubmissionType(subjectEmployeeId, month, submissionType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Submission not found for employee=" + subjectEmployeeId + ", month=" + month + ", type=" + submissionType
                ));

        Map<String, Object> merged = parseJsonAsMap(existing.getPayloadJson());
        deepMerge(merged, payload);
        applyStandardPayloadFields(merged, month, submissionType, subjectEmployeeId);

        String reviewStatus = resolveReviewStatus(merged, existing, true);
        boolean rejected = REVIEW_STATUS_NEEDS_REVIEW.equalsIgnoreCase(reviewStatus);

        existing.setStatus(rejected ? STATUS_DRAFT : STATUS_SUBMITTED);
        existing.setReviewStatus(reviewStatus);
        existing.setPayloadJson(toJson(merged));
        existing.setAdminReviewJson(extractNestedJson(merged, "adminReview", existing.getAdminReviewJson()));
        existing.setManagerReviewJson(extractNestedJson(merged, "managerReview", existing.getManagerReviewJson()));
        existing.setAdminSubmittedAt(LocalDateTime.now());

        MonthlySubmission saved = monthlySubmissionRepository.save(existing);
        log.info("Admin review saved: id={}, employee={}, month={}, actionStatus={}",
                saved.getId(), subjectEmployeeId, month, reviewStatus);
        return toResponse(saved, true);
    }

    private void validatePayload(Map<String, Object> payload, boolean submitting) {
        if (payload == null) throw new IllegalArgumentException("Request payload is required.");
        requireArrayIfPresent(payload, "kpiRatings");
        requireArrayIfPresent(payload, "certifications");
        requireArrayIfPresent(payload, "webknotValueResponses");

        Number recognitions = asNumber(payload.get("recognitionsCount"));
        if (recognitions != null && recognitions.doubleValue() < 0) {
            throw new IllegalArgumentException("Recognitions count cannot be negative.");
        }

        validateRatingsArray(payload.get("kpiRatings"), "kpiRatings");
        validateRatingsArray(payload.get("webknotValueResponses"), "webknotValueResponses");

        if (!submitting) return;

        String managerAction = normalizeUpper(extractAction(payload.get("managerReview")));
        String adminAction = normalizeUpper(extractAction(payload.get("adminReview")));
        String reviewAction = managerAction != null ? managerAction : adminAction;
        if ("REJECT".equals(reviewAction)) {
            String comments = firstNonBlank(
                    extractComments(payload.get("managerReview")),
                    extractComments(payload.get("adminReview")),
                    String.valueOf(payload.getOrDefault("managerComments", "")),
                    String.valueOf(payload.getOrDefault("adminComments", "")),
                    String.valueOf(payload.getOrDefault("managerNotes", "")),
                    String.valueOf(payload.getOrDefault("adminNotes", ""))
            );
            if (comments == null || comments.length() < 10) {
                throw new IllegalArgumentException("Reject comments must be at least 10 characters.");
            }
            return;
        }

        String selfReview = firstNonBlank(
                asString(payload.get("selfReviewText")),
                asString(payload.get("selfReview")),
                asString(payload.get("reviewText"))
        );
        if (selfReview == null) {
            throw new IllegalArgumentException("Self review text is required before submission.");
        }
    }

    private void validateRatingsArray(Object raw, String fieldName) {
        if (!(raw instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            Number rating = asNumber(map.get("rating"));
            if (rating == null) continue;
            double val = rating.doubleValue();
            if (val < 1 || val > 5) {
                throw new IllegalArgumentException(fieldName + " ratings must be between 1 and 5.");
            }
        }
    }

    private void requireArrayIfPresent(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key)) return;
        Object v = payload.get(key);
        if (v == null) return;
        if (!(v instanceof List<?>)) {
            throw new IllegalArgumentException(key + " must be an array.");
        }
    }

    private void applyStandardPayloadFields(Map<String, Object> payload,
                                            String month,
                                            String submissionType,
                                            String subjectEmployeeId) {
        payload.put("month", month);
        payload.put("monthKey", month);
        payload.put("submissionType", submissionType);
        payload.put("employeeId", subjectEmployeeId);
        payload.put("subjectEmployeeId", subjectEmployeeId);
    }

    private String resolveReviewStatus(Map<String, Object> payload, MonthlySubmission current, boolean submitting) {
        String existing = normalizeUpper(
                firstNonBlank(asString(payload.get("reviewStatus")), current != null ? current.getReviewStatus() : null)
        );
        if (!submitting) return existing != null ? existing : STATUS_DRAFT;

        String managerAction = normalizeUpper(extractAction(payload.get("managerReview")));
        String adminAction = normalizeUpper(extractAction(payload.get("adminReview")));

        if ("REJECT".equals(adminAction) || "REJECT".equals(managerAction)) return REVIEW_STATUS_NEEDS_REVIEW;
        if ("APPROVE".equals(adminAction)) return REVIEW_STATUS_ADMIN_APPROVED;
        if ("APPROVE".equals(managerAction)) return REVIEW_STATUS_MANAGER_APPROVED;
        if (existing != null && !STATUS_DRAFT.equals(existing)) return existing;
        return STATUS_SUBMITTED;
    }

    private String resolveMonth(Map<String, Object> payload, String monthFromQuery) {
        String raw = monthFromQuery;
        if (raw == null && payload != null) {
            raw = firstNonBlank(asString(payload.get("month")), asString(payload.get("monthKey")));
        }
        if (raw == null) return null;
        String cleaned = raw.trim();
        try {
            YearMonth parsed = YearMonth.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM"));
            return parsed.toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month. Expected format yyyy-MM.");
        }
    }

    private String resolveSubjectEmployeeId(Map<String, Object> payload, Employee actor) {
        String explicit = payload == null
                ? null
                : firstNonBlank(asString(payload.get("subjectEmployeeId")), asString(payload.get("employeeId")));
        return explicit != null ? explicit : actor.getEmployeeId();
    }

    private String resolveSubmissionType(Map<String, Object> payload, Employee actor, String subjectEmployeeId) {
        String explicit = payload == null
                ? null
                : firstNonBlank(asString(payload.get("submissionType")), asString(payload.get("type")));
        if (explicit != null) return explicit.trim().toUpperCase();
        if (actor.getEmpRole() == EmployeeRole.Manager && actor.getEmployeeId().equalsIgnoreCase(subjectEmployeeId)) {
            return TYPE_MANAGER_SELF;
        }
        return TYPE_EMPLOYEE;
    }

    private MonthlySubmission pickSelfSubmission(List<MonthlySubmission> candidates, Employee actor) {
        if (actor.getEmpRole() == EmployeeRole.Manager) {
            for (MonthlySubmission s : candidates) {
                if (TYPE_MANAGER_SELF.equalsIgnoreCase(s.getSubmissionType())) return s;
            }
        }
        for (MonthlySubmission s : candidates) {
            if (TYPE_EMPLOYEE.equalsIgnoreCase(s.getSubmissionType())) return s;
        }
        return candidates.get(0);
    }

    private Map<String, Object> buildPendingRow(Employee employee, String month) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("month", month);
        payload.put("monthKey", month);
        payload.put("submissionType", TYPE_EMPLOYEE);
        payload.put("selfReviewText", "");
        payload.put("kpiRatings", List.of());
        payload.put("webknotValueResponses", List.of());
        payload.put("certifications", List.of());
        payload.put("recognitionsCount", 0);
        payload.put("employeeId", employee.getEmployeeId());
        payload.put("subjectEmployeeId", employee.getEmployeeId());

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", null);
        row.put("submissionId", null);
        row.put("month", month);
        row.put("status", "NOT_SUBMITTED");
        row.put("reviewStatus", "NOT_SUBMITTED");
        row.put("submissionType", TYPE_EMPLOYEE);
        row.put("subjectEmployeeId", employee.getEmployeeId());
        row.put("employeeId", employee.getEmployeeId());
        row.put("payload", payload);
        row.put("submittedAt", null);
        row.put("updatedAt", employee.getUpdatedAt() != null ? employee.getUpdatedAt().toString() : null);
        row.put("employee", toEmployeeSummary(employee));
        row.put("reopenedForResubmission", false);
        return row;
    }

    private Map<String, Object> toResponse(MonthlySubmission row, boolean includeEmployee) {
        Map<String, Object> payload = parseJsonAsMap(row.getPayloadJson());
        Map<String, Object> out = new LinkedHashMap<>();
        if (row.getManagerReviewJson() != null && !row.getManagerReviewJson().isBlank()) {
            Map<String, Object> managerReview = parseJsonAsMap(row.getManagerReviewJson());
            payload.putIfAbsent("managerReview", managerReview);
            out.put("managerReview", managerReview);
        }
        if (row.getAdminReviewJson() != null && !row.getAdminReviewJson().isBlank()) {
            Map<String, Object> adminReview = parseJsonAsMap(row.getAdminReviewJson());
            payload.putIfAbsent("adminReview", adminReview);
            out.put("adminReview", adminReview);
        }
        payload.putIfAbsent("reviewStatus", row.getReviewStatus());

        out.put("id", row.getId() != null ? String.valueOf(row.getId()) : null);
        out.put("submissionId", row.getId() != null ? String.valueOf(row.getId()) : null);
        out.put("month", row.getMonth());
        out.put("status", row.getStatus());
        out.put("reviewStatus", row.getReviewStatus());
        out.put("submissionType", row.getSubmissionType());
        out.put("subjectEmployeeId", row.getEmployee() != null ? row.getEmployee().getEmployeeId() : null);
        out.put("employeeId", row.getEmployee() != null ? row.getEmployee().getEmployeeId() : null);
        out.put("payload", payload);
        out.put("submittedAt", row.getSubmittedAt() != null ? row.getSubmittedAt().toString() : null);
        out.put("managerSubmittedAt", row.getManagerSubmittedAt() != null ? row.getManagerSubmittedAt().toString() : null);
        out.put("adminSubmittedAt", row.getAdminSubmittedAt() != null ? row.getAdminSubmittedAt().toString() : null);
        out.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : null);
        out.put("createdAt", row.getCreatedAt() != null ? row.getCreatedAt().toString() : null);
        out.put("reopenedForResubmission", REVIEW_STATUS_NEEDS_REVIEW.equalsIgnoreCase(row.getReviewStatus()));
        if (includeEmployee && row.getEmployee() != null) {
            out.put("employee", toEmployeeSummary(row.getEmployee()));
        }
        return out;
    }

    private Map<String, Object> toEmployeeSummary(Employee e) {
        Map<String, Object> employee = new LinkedHashMap<>();
        employee.put("employeeId", e.getEmployeeId());
        employee.put("employeeName", e.getEmployeeName());
        employee.put("email", e.getEmail());
        employee.put("stream", e.getStream());
        employee.put("band", e.getBand() != null ? e.getBand().name() : null);
        employee.put("managerId", e.getManager() != null ? e.getManager().getEmployeeId() : null);
        return employee;
    }

    private Employee requireActor(Authentication authentication) {
        String email = authentication == null ? null : firstNonBlank(authentication.getName());
        if (email == null) throw new AccessDeniedException("Unauthorized");
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    }

    private Employee requireEmployeeById(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
    }

    private void requireAdmin(Employee actor) {
        if (actor.getEmpRole() != EmployeeRole.Admin) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private void requireManagerOrAdmin(Employee actor) {
        if (actor.getEmpRole() != EmployeeRole.Manager && actor.getEmpRole() != EmployeeRole.Admin) {
            throw new AccessDeniedException("Manager access required");
        }
    }

    private Map<String, Object> parseJsonAsMap(String text) {
        if (text == null || text.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(text, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse submission JSON payload, returning empty object: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize submission payload.");
        }
    }

    private Map<String, Object> toMutableMap(Map<String, Object> source) {
        Map<String, Object> input = source == null ? Map.of() : source;
        return new LinkedHashMap<>(input);
    }

    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object existing = target.get(key);
            if (existing instanceof Map<?, ?> existingMap && value instanceof Map<?, ?> incomingMap) {
                Map<String, Object> next = new LinkedHashMap<>((Map<String, Object>) existingMap);
                deepMerge(next, (Map<String, Object>) incomingMap);
                target.put(key, next);
            } else {
                target.put(key, value);
            }
        }
    }

    private String extractNestedJson(Map<String, Object> payload, String key, String fallbackJson) {
        if (payload != null && payload.get(key) instanceof Map<?, ?> map) {
            return toJson(map);
        }
        return fallbackJson;
    }

    private boolean hasNested(Map<String, Object> payload, String key) {
        return payload != null && payload.get(key) instanceof Map<?, ?>;
    }

    private String extractAction(Object reviewRaw) {
        if (!(reviewRaw instanceof Map<?, ?> review)) return null;
        return asString(review.get("action"));
    }

    private String extractComments(Object reviewRaw) {
        if (!(reviewRaw instanceof Map<?, ?> review)) return null;
        return firstNonBlank(
                asString(review.get("comments")),
                asString(review.get("notes")),
                asString(review.get("comment"))
        );
    }

    private String asString(Object raw) {
        if (raw == null) return null;
        String text = String.valueOf(raw).trim();
        return text.isBlank() ? null : text;
    }

    private Number asNumber(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number number) return number;
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeUpper(Object raw) {
        String text = raw == null ? null : String.valueOf(raw).trim();
        return text == null || text.isBlank() ? null : text.toUpperCase();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value == null) continue;
            String v = value.trim();
            if (!v.isBlank()) return v;
        }
        return null;
    }

    private int parseCursorLimit(String raw) {
        if (raw == null || raw.isBlank()) return DEFAULT_CURSOR_LIMIT;
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed <= 0) throw new IllegalArgumentException("Invalid limit. Must be > 0.");
            return Math.min(parsed, MAX_CURSOR_LIMIT);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid limit. Must be a positive integer.");
        }
    }

    private int parseCursorOffset(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < 0) throw new IllegalArgumentException("Invalid cursor. Must be a non-negative integer.");
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid cursor. Must be a non-negative integer.");
        }
    }

    private int parseMaxCyclesPerEmployee(String raw) {
        if (raw == null || raw.isBlank()) return 24;
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException("maxCyclesPerEmployee must be > 0.");
            }
            return Math.min(parsed, 120);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid maxCyclesPerEmployee. Must be a positive integer.");
        }
    }

    private boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "true", "yes", "y" -> true;
            case "0", "false", "no", "n" -> false;
            default -> defaultValue;
        };
    }

    private boolean withinMonthRange(String month, String monthFrom, String monthTo) {
        if (month == null || month.isBlank()) {
            return false;
        }
        if (monthFrom != null && month.compareTo(monthFrom) < 0) {
            return false;
        }
        if (monthTo != null && month.compareTo(monthTo) > 0) {
            return false;
        }
        return true;
    }

    private Set<String> resolveAccessibleEmployeeIds(Employee actor, String employeeFilter) {
        if (actor == null || actor.getEmployeeId() == null) {
            throw new AccessDeniedException("Unauthorized");
        }

        String actorId = actor.getEmployeeId();
        String requestedEmployeeId = firstNonBlank(employeeFilter);

        if (actor.getEmpRole() == EmployeeRole.Admin) {
            if (requestedEmployeeId != null) {
                requireEmployeeById(requestedEmployeeId);
                return Set.of(requestedEmployeeId);
            }
            List<Employee> allEmployees = employeeRepository.findAll();
            Set<String> ids = new LinkedHashSet<>();
            for (Employee employee : allEmployees) {
                if (employee != null && employee.getEmployeeId() != null && !employee.getEmployeeId().isBlank()) {
                    ids.add(employee.getEmployeeId());
                }
            }
            return ids;
        }

        if (actor.getEmpRole() == EmployeeRole.Manager) {
            Set<String> allowed = new LinkedHashSet<>();
            allowed.add(actorId);
            List<Employee> reportees = employeeRepository.findByManager_EmployeeId(actorId);
            for (Employee reportee : reportees) {
                if (reportee != null && reportee.getEmployeeId() != null && !reportee.getEmployeeId().isBlank()) {
                    allowed.add(reportee.getEmployeeId());
                }
            }
            if (requestedEmployeeId == null) {
                return allowed;
            }
            if (!allowed.contains(requestedEmployeeId)) {
                throw new AccessDeniedException("You are not allowed to view this employee's cycle history.");
            }
            return Set.of(requestedEmployeeId);
        }

        if (requestedEmployeeId != null && !actorId.equalsIgnoreCase(requestedEmployeeId)) {
            throw new AccessDeniedException("You are not allowed to view another employee's cycle history.");
        }
        return Set.of(actorId);
    }

    private boolean isSubmittedStatusValue(Object rawStatus) {
        String status = rawStatus == null ? "" : String.valueOf(rawStatus).trim().toUpperCase();
        return "SUBMITTED".equals(status) || "APPROVED".equals(status) || "COMPLETED".equals(status) || "FINAL".equals(status);
    }

    @SuppressWarnings("unchecked")
    private boolean hasManagerReview(Map<String, Object> row) {
        if (row == null) return false;
        if (firstNonBlank(
                row.get("managerSubmittedAt") == null ? null : String.valueOf(row.get("managerSubmittedAt")),
                row.get("managerReviewedAt") == null ? null : String.valueOf(row.get("managerReviewedAt"))
        ) != null) {
            return true;
        }
        Object payloadRaw = row.get("payload");
        if (!(payloadRaw instanceof Map<?, ?> payload)) return false;
        if (payload.get("managerReview") instanceof Map<?, ?>) return true;
        if (payload.get("managerEvaluation") instanceof Map<?, ?>) return true;
        return firstNonBlank(
                payload.get("managerSubmittedAt") == null ? null : String.valueOf(payload.get("managerSubmittedAt")),
                payload.get("managerReviewedAt") == null ? null : String.valueOf(payload.get("managerReviewedAt"))
        ) != null;
    }

    private boolean isEligibleForMonth(Employee employee, YearMonth month) {
        if (employee == null || month == null) return false;
        if (employee.getCreatedAt() == null) return true;
        YearMonth joinedMonth = YearMonth.from(employee.getCreatedAt());
        return !joinedMonth.isAfter(month);
    }

    private void triggerNotificationsForSubmission(Employee actor,
                                                   Employee subject,
                                                   String submissionType,
                                                   Map<String, Object> payload,
                                                   MonthlySubmission saved,
                                                   String month,
                                                   boolean rejected) {
        if (actor == null || subject == null || submissionType == null || saved == null) return;
        if (rejected) return;
        if (!TYPE_EMPLOYEE.equalsIgnoreCase(submissionType)) return;

        boolean actorIsSubject = actor.getEmployeeId() != null && actor.getEmployeeId().equalsIgnoreCase(subject.getEmployeeId());

        if (actor.getEmpRole() == EmployeeRole.Employee && actorIsSubject) {
            notificationService.notifyEmployeeSubmittedToManager(subject, month, saved.getId());
            return;
        }

        String managerAction = normalizeUpper(extractAction(payload == null ? null : payload.get("managerReview")));
        boolean managerReviewSubmitted = "SUBMIT".equals(managerAction) || "APPROVE".equals(managerAction);
        if (actor.getEmpRole() == EmployeeRole.Manager && !actorIsSubject && managerReviewSubmitted) {
            notificationService.notifyManagerEmployeePairSubmittedToAdmins(subject, actor, month, saved.getId());
        }
    }
}
