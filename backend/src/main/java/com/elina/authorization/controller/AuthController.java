package com.elina.authorization.controller;

import com.elina.authorization.dto.LoginRequest;
import com.elina.authorization.dto.LoginResponse;
import com.elina.authorization.dto.RefreshTokenRequest;
import com.elina.authorization.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller for login and token refresh.
 * 
 * Tenant enforcement: Login validates tenant_code before proceeding.
 * All subsequent requests use JWT which contains tenant_id for validation.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.debug("Login attempt for tenant: {}, email: {}", request.getTenantCode(), request.getEmail());
        try {
            LoginResponse response = authService.login(request);
            logger.debug("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.warn("Login failed for tenant: {}, email: {} - {}", request.getTenantCode(), request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * TEMPORARY: Password reset utility endpoint.
     * This should be removed or secured in production.
     * Use this to reset a user's password when you know the email and tenant code.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String tenantCode = request.get("tenantCode");
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        if (tenantCode == null || email == null || newPassword == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "tenantCode, email, and newPassword are required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            authService.resetPassword(tenantCode, email, newPassword);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * TEMPORARY: Diagnostic endpoint to check user details.
     * This should be removed or secured in production.
     */
    @GetMapping("/check-user")
    public ResponseEntity<Map<String, Object>> checkUser(
            @RequestParam String tenantCode,
            @RequestParam String email) {
        try {
            Map<String, Object> userInfo = authService.getUserInfo(tenantCode, email);
            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * TEMPORARY: Test password verification endpoint.
     * This should be removed or secured in production.
     */
    @PostMapping("/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = request.get("hash");
        
        if (password == null || hash == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "password and hash are required");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            Map<String, Object> result = authService.testPasswordMatch(password, hash);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
