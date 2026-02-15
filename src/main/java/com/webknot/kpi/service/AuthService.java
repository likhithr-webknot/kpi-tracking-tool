package com.webknot.kpi.service;

import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.security.JwtService;
import com.webknot.kpi.security.TokenBlacklistService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final long resetTokenExpirationMs;
    private final Map<String, ResetTokenData> resetTokens = new ConcurrentHashMap<>();
    private final Logger log = LogManager.getLogger(AuthService.class);

    public AuthService(EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService,
                       @Value("${auth.reset-token-expiration-ms:900000}") long resetTokenExpirationMs) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.resetTokenExpirationMs = resetTokenExpirationMs;
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
        employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with this email"));

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(resetTokenExpirationMs);
        resetTokens.put(token, new ResetTokenData(email, expiresAt));
        log.info("Password reset token generated for email: {}", email);

        return new ForgotPasswordResult(
                "Password reset token generated successfully",
                token,
                expiresAt
        );
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        ResetTokenData tokenData = resetTokens.get(token);
        if (tokenData == null) {
            throw new IllegalArgumentException("Invalid reset token");
        }
        if (tokenData.expiresAt().isBefore(Instant.now())) {
            resetTokens.remove(token);
            throw new IllegalArgumentException("Reset token has expired");
        }

        Employee employee = employeeRepository.findByEmail(tokenData.email())
                .orElseThrow(() -> new IllegalArgumentException("No user found with this email"));

        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        resetTokens.remove(token);
        log.info("Password reset successful for email: {}", employee.getEmail());
    }

    public record AuthResult(String accessToken, String role, String portal) {}
    public record ForgotPasswordResult(String message, String resetToken, Instant expiresAt) {}
    private record ResetTokenData(String email, Instant expiresAt) {}
}
