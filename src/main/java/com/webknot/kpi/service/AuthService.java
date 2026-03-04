package com.webknot.kpi.service;

import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.security.JwtService;
import com.webknot.kpi.security.TokenBlacklistService;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final String RESET_REQUEST_PREFIX = "auth:reset:";
    private final EmployeeRepository employeeRepository;
    private final DesignationLookupRepository designationLookupRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final NotificationService notificationService;
    private final long resetTokenExpirationMs;
    private final Map<String, AdminResetRequestData> adminResetRequests = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final boolean redisEnabled;
    private final Logger log = LogManager.getLogger(AuthService.class);

    public AuthService(EmployeeRepository employeeRepository,
                       DesignationLookupRepository designationLookupRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService,
                       NotificationService notificationService,
                       @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                       @Value("${auth.reset-token-expiration-ms:900000}") long resetTokenExpirationMs) {
        this.employeeRepository = employeeRepository;
        this.designationLookupRepository = designationLookupRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.notificationService = notificationService;
        this.resetTokenExpirationMs = resetTokenExpirationMs;
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.redisEnabled = isRedisAvailable();
    }

    private boolean isRedisAvailable() {
        if (redisTemplate.isEmpty()) {
            return false;
        }
        try {
            var connectionFactory = redisTemplate.get().getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            try (var connection = connectionFactory.getConnection()) {
                connection.ping();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public AuthResult login(String email, String password) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String role = employee.getEmpRole() != null ? employee.getEmpRole().name() : EmployeeRole.Employee.name();
        String portal = switch (employee.getEmpRole() != null ? employee.getEmpRole() : EmployeeRole.Employee) {
            case Admin -> "/portal/admin";
            case Manager -> "/portal/manager";
            case Employee -> "/portal/employee";
        };

        log.info("JWT issued for email: {}, role: {}", employee.getEmail(), role);
        return new AuthResult(jwtService.generateToken(employee), role, portal);
    }

    public void logout(String token) {
        tokenBlacklistService.revokeToken(token, jwtService.extractExpiration(token));
        log.info("JWT revoked successfully");
    }

    public ForgotPasswordResult forgotPassword(String email) {
        purgeExpiredResetRequests();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with this email"));
        var admins = employeeRepository.findByEmpRole(EmployeeRole.Admin);
        if (admins == null || admins.isEmpty()) {
            throw new IllegalArgumentException("No admin accounts configured to approve password reset.");
        }

        String requestId = UUID.randomUUID().toString();
        String adminCode = generateAdminCode();
        Instant expiresAt = Instant.now().plusMillis(resetTokenExpirationMs);
        
        AdminResetRequestData requestData = new AdminResetRequestData(employee.getEmail(), passwordEncoder.encode(adminCode), expiresAt);
        
        if (redisEnabled) {
            long ttlSeconds = TimeUnit.MILLISECONDS.toSeconds(resetTokenExpirationMs);
            redisTemplate.ifPresent(rt -> rt.opsForValue().set(RESET_REQUEST_PREFIX + requestId, requestData, ttlSeconds, TimeUnit.SECONDS));
        }
        
        adminResetRequests.put(requestId, requestData);

        String adminEmails = admins.stream()
                .map(Employee::getEmail)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("(no-email-admin)");

        log.warn(
                "PASSWORD RESET APPROVAL CODE | requestId={} | targetEmail={} | admins={} | code={} | expiresAt={}",
                requestId,
                employee.getEmail(),
                adminEmails,
                adminCode,
                expiresAt
        );
        notificationService.notifyForgotPasswordRequested(employee, requestId, adminCode, expiresAt, admins);

        return new ForgotPasswordResult(
                "Password reset request submitted. An admin verification code has been sent to administrators.",
                requestId,
                expiresAt
        );
    }

    public void resetPassword(String token, String newPassword) {
        throw new IllegalArgumentException("Direct password reset is disabled. Please use admin-approved reset flow.");
    }

    public void adminResetPassword(String actorEmail, String requestId, String adminCode, String newPassword) {
        purgeExpiredResetRequests();

        Employee admin = employeeRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Unauthorized"));
        if (admin.getEmpRole() != EmployeeRole.Admin) {
            throw new IllegalArgumentException("Admin access required");
        }
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Reset request id is required");
        }
        if (adminCode == null || adminCode.isBlank()) {
            throw new IllegalArgumentException("Admin verification code is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        String normalizedRequestId = requestId.trim();
        String normalizedCode = adminCode.trim();
        
        AdminResetRequestData requestData = null;
        
        if (redisEnabled) {
            requestData = redisTemplate.map(rt -> {
                Object redisData = rt.opsForValue().get(RESET_REQUEST_PREFIX + normalizedRequestId);
                return redisData instanceof AdminResetRequestData ? (AdminResetRequestData) redisData : null;
            }).orElse(null);
        }
        
        if (requestData == null) {
            requestData = adminResetRequests.get(normalizedRequestId);
        }
        
        if (requestData == null) {
            throw new IllegalArgumentException("Invalid reset request id");
        }
        if (requestData.expiresAt().isBefore(Instant.now())) {
            adminResetRequests.remove(normalizedRequestId);
            redisTemplate.ifPresent(rt -> rt.delete(RESET_REQUEST_PREFIX + normalizedRequestId));
            throw new IllegalArgumentException("Reset request has expired");
        }
        if (!passwordEncoder.matches(normalizedCode, requestData.adminCodeHash())) {
            throw new IllegalArgumentException("Invalid admin verification code");
        }

        Employee employee = employeeRepository.findByEmail(requestData.email())
                .orElseThrow(() -> new IllegalArgumentException("No user found with this email"));

        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        adminResetRequests.remove(normalizedRequestId);
        redisTemplate.ifPresent(rt -> rt.delete(RESET_REQUEST_PREFIX + normalizedRequestId));
        log.info(
                "Password reset successful for email={} by admin={} using requestId={}",
                employee.getEmail(),
                admin.getEmail(),
                normalizedRequestId
        );
    }

    @Transactional(readOnly = true, timeout = 10)
    public MeResponse getCurrentUser(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        EmployeeRole role = employee.getEmpRole() != null ? employee.getEmpRole() : EmployeeRole.Employee;
        String portal = switch (role) {
            case Admin -> "/portal/admin";
            case Manager -> "/portal/manager";
            case Employee -> "/portal/employee";
        };
        return new MeResponse(
                employee.getEmployeeId(),
                employee.getEmployeeName(),
                employee.getEmail(),
                role.name(),
                resolveDesignation(employee),
                employee.getStream(),
                employee.getBand() != null ? employee.getBand().name() : null,
                employee.getManager() != null ? employee.getManager().getEmployeeId() : null,
                employee.getUpdatedBy() != null ? employee.getUpdatedBy().getEmployeeId() : null,
                employee.getCreatedAt(),
                employee.getUpdatedAt(),
                portal
        );
    }

    public record AuthResult(String accessToken, String role, String portal) {}
    public record ForgotPasswordResult(String message, String requestId, Instant expiresAt) {}
    public record MeResponse(
            String employeeId,
            String employeeName,
            String email,
            String role,
            String designation,
            String stream,
            String band,
            String managerId,
            String updatedById,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            String portal
    ) {}

    private String resolveDesignation(Employee employee) {
        if (employee == null || employee.getBand() == null || employee.getStream() == null || employee.getStream().isBlank()) {
            return null;
        }
        String canonicalStream = BandStreamNormalizer.canonicalStreamLabel(employee.getStream());
        if (canonicalStream != null && !canonicalStream.isBlank()) {
            DesignationLookup.DesignationId canonicalId = new DesignationLookup.DesignationId(canonicalStream, employee.getBand());
            var canonical = designationLookupRepository.findById(canonicalId).map(DesignationLookup::getDesignation);
            if (canonical.isPresent()) return canonical.get();
        }
        DesignationLookup.DesignationId rawId = new DesignationLookup.DesignationId(employee.getStream(), employee.getBand());
        return designationLookupRepository.findById(rawId).map(DesignationLookup::getDesignation).orElse(null);
    }
    private String generateAdminCode() {
        int value = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(value);
    }

    private void purgeExpiredResetRequests() {
        Instant now = Instant.now();
        adminResetRequests.entrySet().removeIf(entry -> {
            AdminResetRequestData data = entry.getValue();
            return data == null || data.expiresAt().isBefore(now);
        });
    }

    private record AdminResetRequestData(String email, String adminCodeHash, Instant expiresAt) {}
}
