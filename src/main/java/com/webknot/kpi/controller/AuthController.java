package com.webknot.kpi.controller;

import com.webknot.kpi.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final Logger log = LogManager.getLogger(AuthController.class);
    private final String authCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long cookieMaxAgeSeconds;

    public AuthController(AuthService authService,
                          @Value("${auth.cookie.name:access_token}") String authCookieName,
                          @Value("${auth.cookie.secure:false}") boolean cookieSecure,
                          @Value("${auth.cookie.same-site:Lax}") String cookieSameSite,
                          @Value("${auth.cookie.max-age-seconds:86400}") long cookieMaxAgeSeconds) {
        this.authService = authService;
        this.authCookieName = authCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.AuthResult result = authService.login(request.email(), request.password());
            ResponseCookie cookie = ResponseCookie.from(authCookieName, result.accessToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite(cookieSameSite)
                    .maxAge(cookieMaxAgeSeconds)
                    .build();
            log.info("Login successful for email: {}", request.email());
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(new LoginResponse("Bearer", result.role(), result.portal()));
        } catch (BadCredentialsException e) {
            log.warn("Login failed for email: {}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authorization,
                                    @CookieValue(name = "${auth.cookie.name:access_token}", required = false) String cookieToken) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else if (cookieToken != null && !cookieToken.isBlank()) {
            token = cookieToken;
        }
        ResponseCookie clearCookie = ResponseCookie.from(authCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(0)
                .build();
        if (token == null || token.isBlank()) {
            log.warn("Logout failed due to missing token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Set-Cookie", clearCookie.toString())
                    .body("Missing token");
        }
        try {
            authService.logout(token);
            log.info("Logout successful");
            return ResponseEntity.ok()
                    .header("Set-Cookie", clearCookie.toString())
                    .body("Logged out successfully");
        } catch (Exception e) {
            log.warn("Logout failed due to invalid token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Set-Cookie", clearCookie.toString())
                    .body("Invalid token");
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

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        try {
            return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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

    public record LoginResponse(String tokenType, String role, String portal) {}
    public record ForgotPasswordResponse(String message, String resetToken, String expiresAt) {}
}
