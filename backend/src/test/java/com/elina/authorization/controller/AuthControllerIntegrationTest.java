package com.elina.authorization.controller;

import com.elina.authorization.dto.LoginRequest;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.entity.User;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for login endpoint.
 * Tests the complete login flow from HTTP request to JWT generation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        // Create test tenant
        tenant = new Tenant();
        tenant.setTenantCode("TEST");
        tenant.setName("Test Tenant");
        tenant.setClientCode("TEST001");
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);

        // Create test user
        user = new User();
        user.setTenant(tenant);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("Test@123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setIsActive(true);
        user = userRepository.save(user);
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setTenantCode("TEST");
        request.setEmail("test@example.com");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.userProfile.email").value("test@example.com"))
                .andExpect(jsonPath("$.tenantInfo.tenantCode").value("TEST"));
    }

    @Test
    void testLogin_InvalidTenantCode() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setTenantCode("INVALID");
        request.setEmail("test@example.com");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_InvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setTenantCode("TEST");
        request.setEmail("test@example.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

