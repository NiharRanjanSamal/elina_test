package com.elina.projects.controller;

import com.elina.authorization.context.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.entity.User;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.repository.UserRepository;
import com.elina.authorization.security.JwtTokenProvider;
import com.elina.projects.entity.PlanLine;
import com.elina.projects.entity.PlanVersion;
import com.elina.projects.entity.Project;
import com.elina.projects.entity.Task;
import com.elina.projects.entity.Wbs;
import com.elina.projects.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PlanController.
 * Tests all 3 creation modes, version comparison, and CRUD operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlanControllerIntegrationTest {

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

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WbsRepository wbsRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PlanVersionRepository planVersionRepository;

    @Autowired
    private PlanLineRepository planLineRepository;

    private Tenant tenant;
    private User user;
    private Project project;
    private Wbs wbs;
    private Task task;
    private String authToken;

    @BeforeEach
    void setUp() {
        // Clear any existing context to ensure clean state
        // Note: Don't clear SecurityContext here - let TenantFilter set it from JWT
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        
        // Create test tenant
        tenant = new Tenant();
        tenant.setTenantCode("TEST");
        tenant.setName("Test Tenant");
        tenant.setClientCode("TEST001");
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);
        TenantContext.setTenantId(tenant.getId());

        // Create test user
        user = new User();
        user.setTenant(tenant);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("Test@123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setIsActive(true);
        user = userRepository.save(user);

        // Generate JWT token with required permissions
        List<String> roles = new ArrayList<>();
        roles.add("ROLE_SYSTEM_ADMIN"); // Admin role bypasses permission checks
        
        List<String> permissions = new ArrayList<>();
        permissions.add("PAGE_PROJECTS_VIEW");
        permissions.add("PAGE_PROJECTS_EDIT");
        
        authToken = tokenProvider.generateToken(user.getId(), tenant.getId(), roles, permissions);

        // Create test project
        project = new Project();
        project.setTenant(tenant);
        project.setProjectCode("PROJ001");
        project.setProjectName("Test Project");
        project.setStartDate(LocalDate.of(2025, 1, 1));
        project.setEndDate(LocalDate.of(2025, 12, 31));
        project.setActivateFlag(true);
        project.setCreatedBy(user.getId());
        project = projectRepository.save(project);

        // Create test WBS
        wbs = new Wbs();
        wbs.setTenant(tenant);
        wbs.setProject(project);
        wbs.setWbsCode("WBS001");
        wbs.setWbsName("Test WBS");
        wbs.setStartDate(LocalDate.of(2025, 1, 1));
        wbs.setEndDate(LocalDate.of(2025, 3, 31));
        wbs.setLevel(1);
        wbs.setActivateFlag(true);
        wbs.setCreatedBy(user.getId());
        wbs = wbsRepository.save(wbs);

        // Create test task
        task = new Task();
        task.setTenant(tenant);
        task.setProject(project);
        task.setWbs(wbs);
        task.setTaskCode("TASK001");
        task.setTaskName("Test Task");
        task.setStartDate(LocalDate.of(2025, 1, 1));
        task.setEndDate(LocalDate.of(2025, 1, 31));
        task.setPlannedQty(BigDecimal.valueOf(1000.0));
        task.setUnit("M3");
        task.setActivateFlag(true);
        task.setCreatedBy(user.getId());
        task = taskRepository.save(task);
    }

    @AfterEach
    void tearDown() {
        // Clear TenantContext and SecurityContext after each test for proper isolation
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ========== DAILY ENTRY MODE ==========

    @Test
    void testCreatePlanVersion_DailyEntryMode_Success() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "DAILY_ENTRY");
        payload.put("description", "Test Daily Entry Plan");

        List<Map<String, Object>> dailyLines = new ArrayList<>();
        Map<String, Object> line1 = new HashMap<>();
        line1.put("plannedDate", LocalDate.of(2025, 1, 10).toString());
        line1.put("plannedQty", 50.0);
        dailyLines.add(line1);

        Map<String, Object> line2 = new HashMap<>();
        line2.put("plannedDate", LocalDate.of(2025, 1, 15).toString());
        line2.put("plannedQty", 75.0);
        dailyLines.add(line2);

        payload.put("dailyLines", dailyLines);

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").exists())
                .andExpect(jsonPath("$.versionNo").value(1));
    }

    @Test
    void testCreatePlanVersion_DailyEntryMode_EmptyLines_ReturnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "DAILY_ENTRY");
        payload.put("dailyLines", new ArrayList<>());

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ========== DATE RANGE SPLIT MODE ==========

    @Test
    void testCreatePlanVersion_DateRangeSplitMode_EqualSplit_Success() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "DATE_RANGE_SPLIT");

        Map<String, Object> rangeSplit = new HashMap<>();
        rangeSplit.put("startDate", LocalDate.of(2025, 1, 1).toString());
        rangeSplit.put("endDate", LocalDate.of(2025, 1, 10).toString());
        rangeSplit.put("totalQty", 100.0);
        rangeSplit.put("splitType", "EQUAL_SPLIT");

        payload.put("rangeSplit", rangeSplit);

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").exists());
    }

    @Test
    void testCreatePlanVersion_DateRangeSplitMode_WeeklySplit_Success() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "DATE_RANGE_SPLIT");

        Map<String, Object> rangeSplit = new HashMap<>();
        rangeSplit.put("startDate", LocalDate.of(2025, 1, 1).toString());
        rangeSplit.put("endDate", LocalDate.of(2025, 1, 14).toString());
        rangeSplit.put("totalQty", 140.0);
        rangeSplit.put("splitType", "WEEKLY_SPLIT");

        payload.put("rangeSplit", rangeSplit);

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    void testCreatePlanVersion_DateRangeSplitMode_CustomSplit_Success() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "DATE_RANGE_SPLIT");

        Map<String, Object> rangeSplit = new HashMap<>();
        rangeSplit.put("startDate", LocalDate.of(2025, 1, 1).toString());
        rangeSplit.put("endDate", LocalDate.of(2025, 1, 10).toString());
        rangeSplit.put("totalQty", 100.0);
        rangeSplit.put("splitType", "CUSTOM_SPLIT");
        rangeSplit.put("splitCount", 2);
        rangeSplit.put("customQuantities", List.of(60.0, 40.0));

        payload.put("rangeSplit", rangeSplit);

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    void testCreatePlanVersion_DateRangeSplitMode_InvalidRange_ReturnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "DATE_RANGE_SPLIT");

        Map<String, Object> rangeSplit = new HashMap<>();
        rangeSplit.put("startDate", LocalDate.of(2025, 1, 10).toString());
        rangeSplit.put("endDate", LocalDate.of(2025, 1, 1).toString()); // Invalid range
        rangeSplit.put("totalQty", 100.0);
        rangeSplit.put("splitType", "EQUAL_SPLIT");

        payload.put("rangeSplit", rangeSplit);

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ========== SINGLE LINE QUICK MODE ==========

    @Test
    @Order(100) // Run this test last to avoid test isolation issues
    void testCreatePlanVersion_SingleLineQuickMode_Success() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("versionDate", LocalDate.now().toString());
        payload.put("mode", "SINGLE_LINE_QUICK");

        Map<String, Object> singleLine = new HashMap<>();
        singleLine.put("plannedDate", LocalDate.of(2025, 1, 15).toString());
        singleLine.put("plannedQty", 100.0);
        singleLine.put("description", "Quick plan");

        payload.put("singleLine", singleLine);

        mockMvc.perform(post("/api/plans/create-with-mode")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").exists());
    }

    // ========== VERSION COMPARISON ==========

    @Test
    void testComparePlanVersions_Success() throws Exception {
        // Create first version
        PlanVersion version1 = new PlanVersion();
        version1.setTenant(tenant);
        version1.setTask(task);
        version1.setVersionNo(1);
        version1.setVersionDate(LocalDate.now());
        version1.setIsActive(true);
        version1.setActivateFlag(true);
        version1.setCreatedBy(user.getId());
        version1 = planVersionRepository.save(version1);

        PlanLine line1 = new PlanLine();
        line1.setTenant(tenant);
        line1.setPlanVersion(version1);
        line1.setTask(task);
        line1.setLineNumber(1);
        line1.setWorkDate(LocalDate.of(2025, 1, 10));
        line1.setPlannedQty(BigDecimal.valueOf(100.0));
        line1.setActivateFlag(true);
        line1.setCreatedBy(user.getId());
        planLineRepository.save(line1);

        // Create second version
        PlanVersion version2 = new PlanVersion();
        version2.setTenant(tenant);
        version2.setTask(task);
        version2.setVersionNo(2);
        version2.setVersionDate(LocalDate.now());
        version2.setIsActive(false);
        version2.setActivateFlag(true);
        version2.setCreatedBy(user.getId());
        version2 = planVersionRepository.save(version2);

        PlanLine line2 = new PlanLine();
        line2.setTenant(tenant);
        line2.setPlanVersion(version2);
        line2.setTask(task);
        line2.setLineNumber(1);
        line2.setWorkDate(LocalDate.of(2025, 1, 10));
        line2.setPlannedQty(BigDecimal.valueOf(150.0));
        line2.setActivateFlag(true);
        line2.setCreatedBy(user.getId());
        planLineRepository.save(line2);

        mockMvc.perform(get("/api/plans/compare/{versionId1}/{versionId2}", version1.getPlanVersionId(), version2.getPlanVersionId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version1").exists())
                .andExpect(jsonPath("$.version2").exists())
                .andExpect(jsonPath("$.comparisonLines").exists())
                .andExpect(jsonPath("$.summary").exists());
    }

    // ========== LIST VERSIONS ==========

    @Test
    void testListPlanVersions_Success() throws Exception {
        // Create test version
        PlanVersion version = new PlanVersion();
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(1);
        version.setVersionDate(LocalDate.now());
        version.setIsActive(true);
        version.setActivateFlag(true);
        version.setCreatedBy(user.getId());
        planVersionRepository.save(version);

        mockMvc.perform(get("/api/plans/task/{taskId}", task.getTaskId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].versionNo").exists());
    }

    // ========== SET ACTIVE VERSION ==========

    @Test
    void testSetActiveVersion_Success() throws Exception {
        // Create test version
        PlanVersion version = new PlanVersion();
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(1);
        version.setVersionDate(LocalDate.now());
        version.setIsActive(false);
        version.setActivateFlag(true);
        version.setCreatedBy(user.getId());
        version = planVersionRepository.save(version);

        mockMvc.perform(put("/api/plans/{id}/activate", version.getPlanVersionId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    // ========== REVERT VERSION ==========

    @Test
    void testRevertToPlanVersion_Success() throws Exception {
        // Create test version
        PlanVersion version = new PlanVersion();
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(1);
        version.setVersionDate(LocalDate.now());
        version.setIsActive(false);
        version.setActivateFlag(true);
        version.setCreatedBy(user.getId());
        version = planVersionRepository.save(version);

        mockMvc.perform(put("/api/plans/{id}/revert", version.getPlanVersionId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }
}

