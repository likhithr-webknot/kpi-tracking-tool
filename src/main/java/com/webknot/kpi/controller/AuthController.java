package com.webknot.kpi.controller;

import com.webknot.kpi.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final Logger log = LogManager.getLogger(AuthController.class);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.AuthResult result = authService.login(request.email(), request.password());
            log.info("Login successful for email: {}", request.email());
            return ResponseEntity.ok(new LoginResponse(
                    result.accessToken(),
                    "Bearer",
                    result.role(),
                    result.portal()
            ));
        } catch (BadCredentialsException e) {
            log.warn("Login failed for email: {}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Logout failed due to missing/invalid Authorization header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing or invalid Authorization header");
        }
        String token = authorization.substring(7);
        try {
            authService.logout(token);
            log.info("Logout successful");
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            log.warn("Logout failed due to invalid token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            AuthService.ForgotPasswordResult result = authService.forgotPassword(request.email());
            log.info("Forgot password requested for email: {}", request.email());
            return ResponseEntity.ok(new ForgotPasswordResponse(
                    result.message(),
                    result.resetToken(),
                    result.expiresAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Forgot password failed for email: {}", request.email());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.token(), request.newPassword());
            log.info("Password reset completed");
            return ResponseEntity.ok("Password reset successful");
        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank String newPassword
    ) {}

    public record LoginResponse(String accessToken, String tokenType, String role, String portal) {}
    public record ForgotPasswordResponse(String message, String resetToken, String expiresAt) {}
}
