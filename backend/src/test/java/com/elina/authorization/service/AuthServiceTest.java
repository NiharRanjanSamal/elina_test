package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.dto.LoginRequest;
import com.elina.authorization.dto.LoginResponse;
import com.elina.authorization.entity.*;
import com.elina.authorization.repository.*;
import com.elina.authorization.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests login and token refresh functionality with tenant enforcement.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private Tenant tenant;
    private User user;
    private Role role;
    private UserRole userRole;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("DEFAULT");
        tenant.setName("Default Tenant");
        tenant.setClientCode("CLIENT001");
        tenant.setIsActive(true);

        user = new User();
        user.setId(1L);
        user.setTenant(tenant);
        user.setEmail("admin@example.com");
        user.setPasswordHash("$2a$10$hashedPassword");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setIsActive(true);

        role = new Role();
        role.setId(1L);
        role.setCode("SYSTEM_ADMIN");
        role.setName("System Administrator");
        role.setIsActive(true);
        role.setTenant(tenant);

        userRole = new UserRole();
        userRole.setId(1L);
        userRole.setUser(user);
        userRole.setRole(role);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setTenantCode("DEFAULT");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        when(tenantRepository.findByTenantCode("DEFAULT")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId("admin@example.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(userRoleRepository.findByUserId(1L)).thenReturn(Arrays.asList(userRole));
        when(roleRepository.findByCodeAndTenantId("SYSTEM_ADMIN", 1L)).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(1L)).thenReturn(Arrays.asList());
        when(userPermissionRepository.findByUserId(1L)).thenReturn(Arrays.asList());
        when(tokenProvider.generateToken(anyLong(), anyLong(), anyList(), anyList())).thenReturn("jwt-token");
        when(tokenProvider.generateRefreshToken(anyLong(), anyLong())).thenReturn("refresh-token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getUserProfile());
        assertEquals("admin@example.com", response.getUserProfile().getEmail());
        assertNotNull(response.getTenantInfo());
        assertEquals("DEFAULT", response.getTenantInfo().getTenantCode());

        verify(userRepository).save(any(User.class));
        verify(tenantRepository).findByTenantCode("DEFAULT");
    }

    @Test
    void testLogin_InvalidTenantCode() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setTenantCode("INVALID");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        when(tenantRepository.findByTenantCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setTenantCode("DEFAULT");
        request.setEmail("admin@example.com");
        request.setPassword("wrongpassword");

        when(tenantRepository.findByTenantCode("DEFAULT")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId("admin@example.com", 1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(request));
    }
}