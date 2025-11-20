package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.dto.LoginRequest;
import com.elina.authorization.dto.LoginResponse;
import com.elina.authorization.entity.*;
import com.elina.authorization.repository.*;
import com.elina.authorization.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authentication service for login and token refresh.
 * 
 * Tenant enforcement: Login validates tenant_code and sets tenant context.
 * All subsequent operations use TenantContext for tenant isolation.
 * 
 * To reuse in other systems: Update validation logic if needed and ensure
 * tenant_code validation matches your requirements.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserPermissionRepository userPermissionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Normalize email (trim and lowercase for case-insensitive comparison)
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;
        
        // Validate tenant code
        Tenant tenant = tenantRepository.findByTenantCode(request.getTenantCode())
                .orElseThrow(() -> {
                    logger.warn("Login failed: Tenant code '{}' not found", request.getTenantCode());
                    return new RuntimeException("Invalid tenant code: " + request.getTenantCode());
                });

        if (!tenant.getIsActive()) {
            logger.warn("Login failed: Tenant '{}' is not active", request.getTenantCode());
            throw new RuntimeException("Tenant is not active");
        }

        // Find user by email and tenant (case-insensitive)
        User user = userRepository.findByEmailAndTenantId(normalizedEmail, tenant.getId())
                .orElseThrow(() -> {
                    logger.warn("Login failed: User with email '{}' not found for tenant '{}'", normalizedEmail, request.getTenantCode());
                    return new RuntimeException("Invalid email or password");
                });

        if (!user.getIsActive()) {
            logger.warn("Login failed: User '{}' is not active", normalizedEmail);
            throw new RuntimeException("User is not active");
        }

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            logger.warn("Login failed: Password mismatch for user '{}' in tenant '{}'", normalizedEmail, request.getTenantCode());
            throw new RuntimeException("Invalid email or password");
        }

        // Update last login
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Set tenant context for this request
        TenantContext.setTenantId(tenant.getId());

        // Get user roles and permissions
        List<String> roles = getUserRoles(user.getId());
        List<String> permissions = getUserPermissions(user.getId(), roles);

        // Generate tokens
        String token = tokenProvider.generateToken(user.getId(), tenant.getId(), roles, permissions);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), tenant.getId());

        // Build response
        LoginResponse.UserProfile userProfile = new LoginResponse.UserProfile();
        userProfile.setId(user.getId());
        userProfile.setEmail(user.getEmail());
        userProfile.setFirstName(user.getFirstName());
        userProfile.setLastName(user.getLastName());
        userProfile.setFullName(user.getFullName());
        userProfile.setRoles(roles);
        userProfile.setPermissions(permissions);

        LoginResponse.TenantInfo tenantInfo = new LoginResponse.TenantInfo();
        tenantInfo.setId(tenant.getId());
        tenantInfo.setTenantCode(tenant.getTenantCode());
        tenantInfo.setName(tenant.getName());
        tenantInfo.setClientCode(tenant.getClientCode());

        return new LoginResponse(token, refreshToken, userProfile, tenantInfo);
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        Long tenantId = tokenProvider.getTenantIdFromToken(refreshToken);

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            throw new RuntimeException("User is not active");
        }

        Tenant tenant = user.getTenant();
        if (!tenant.getIsActive()) {
            throw new RuntimeException("Tenant is not active");
        }

        // Set tenant context
        TenantContext.setTenantId(tenantId);

        // Get user roles and permissions
        List<String> roles = getUserRoles(user.getId());
        List<String> permissions = getUserPermissions(user.getId(), roles);

        // Generate new tokens
        String newToken = tokenProvider.generateToken(user.getId(), tenantId, roles, permissions);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId(), tenantId);

        // Build response
        LoginResponse.UserProfile userProfile = new LoginResponse.UserProfile();
        userProfile.setId(user.getId());
        userProfile.setEmail(user.getEmail());
        userProfile.setFirstName(user.getFirstName());
        userProfile.setLastName(user.getLastName());
        userProfile.setFullName(user.getFullName());
        userProfile.setRoles(roles);
        userProfile.setPermissions(permissions);

        LoginResponse.TenantInfo tenantInfo = new LoginResponse.TenantInfo();
        tenantInfo.setId(tenant.getId());
        tenantInfo.setTenantCode(tenant.getTenantCode());
        tenantInfo.setName(tenant.getName());
        tenantInfo.setClientCode(tenant.getClientCode());

        return new LoginResponse(newToken, newRefreshToken, userProfile, tenantInfo);
    }

    private List<String> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
                .filter(ur -> ur.getRole().getIsActive())
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toList());
    }

    private List<String> getUserPermissions(Long userId, List<String> roles) {
        Long tenantId = TenantContext.getTenantId();
        
        // Get permissions from roles
        List<String> rolePermissions = roles.stream()
                .flatMap(roleCode -> {
                    // Find role by code and tenant
                    Role role = roleRepository.findByCodeAndTenantId(roleCode, tenantId)
                            .orElse(null);
                    
                    if (role == null) {
                        return java.util.stream.Stream.<String>empty();
                    }
                    
                    // Get permissions for this role
                    return rolePermissionRepository.findByRoleId(role.getId()).stream()
                            .map(RolePermission::getPermission)
                            .filter(p -> p.getIsActive())
                            .map(Permission::getCode);
                })
                .distinct()
                .collect(Collectors.toList());

        // Get direct user permissions
        List<String> directPermissions = userPermissionRepository.findByUserId(userId).stream()
                .filter(up -> up.getPermission().getIsActive())
                .map(up -> up.getPermission().getCode())
                .collect(Collectors.toList());

        // Combine and deduplicate
        rolePermissions.addAll(directPermissions);
        return rolePermissions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * TEMPORARY: Reset password for a user by tenant code and email.
     * This should be removed or secured in production.
     */
    @Transactional
    public void resetPassword(String tenantCode, String email, String newPassword) {
        // Normalize email
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        
        // Find tenant
        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Find user
        User user = userRepository.findByEmailAndTenantId(normalizedEmail, tenant.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * TEMPORARY: Get user info for diagnostics.
     * This should be removed or secured in production.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserInfo(String tenantCode, String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        
        // Find tenant
        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Find user
        User user = userRepository.findByEmailAndTenantId(normalizedEmail, tenant.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> info = new HashMap<>();
        info.put("email", user.getEmail());
        info.put("tenantCode", tenant.getTenantCode());
        info.put("tenantId", tenant.getId());
        info.put("isActive", user.getIsActive());
        info.put("tenantIsActive", tenant.getIsActive());
        info.put("hasPasswordHash", user.getPasswordHash() != null && !user.getPasswordHash().isEmpty());
        info.put("passwordHashLength", user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);
        info.put("passwordHashPrefix", user.getPasswordHash() != null && user.getPasswordHash().length() > 10 
                ? user.getPasswordHash().substring(0, 10) + "..." : "null");
        
        return info;
    }

    /**
     * TEMPORARY: Test password matching.
     * This should be removed or secured in production.
     */
    public Map<String, Object> testPasswordMatch(String password, String hash) {
        boolean matches = passwordEncoder.matches(password, hash);
        
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", matches);
        result.put("hashLength", hash != null ? hash.length() : 0);
        result.put("hashPrefix", hash != null && hash.length() > 10 ? hash.substring(0, 10) + "..." : "null");
        
        if (!matches) {
            // Generate a new hash for the password
            String newHash = passwordEncoder.encode(password);
            result.put("newHash", newHash);
            result.put("message", "Password does NOT match. New hash generated above.");
        } else {
            result.put("message", "Password matches correctly!");
        }
        
        return result;
    }
}

