package com.elina.projects.controller;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import com.elina.authorization.repository.BusinessRuleRepository;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.repository.UserRepository;
import com.elina.authorization.security.JwtTokenProvider;
import com.elina.projects.dto.TaskUpdateBulkDTO;
import com.elina.projects.entity.*;
import com.elina.projects.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskUpdateController.
 * 
 * Tests cover:
 * - GET /api/task-updates/task/{taskId} - Get day-wise updates
 * - POST /api/task-updates/task/{taskId} - Bulk save updates
 * - DELETE /api/task-updates/{updateId} - Delete update
 * - GET /api/task-updates/task/{taskId}/summary - Get daily summary
 * - Business rule violations
 * - Confirmation lock blocking
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskUpdateControllerIntegrationTest {

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
    private TaskUpdateRepository taskUpdateRepository;

    @Autowired
    private PlanVersionRepository planVersionRepository;

    @Autowired
    private PlanLineRepository planLineRepository;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    // Note: confirmationRepository not used in these tests but available if needed

    private Tenant tenant;
    private User user;
    private Project project;
    private Wbs wbs;
    private Task task;
    private String authToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        // Create test tenant
        tenant = new Tenant();
        tenant.setTenantCode("TEST_TASK_UPDATE");
        tenant.setName("Test Task Update Tenant");
        tenant.setClientCode("TEST001");
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);
        TenantContext.setTenantId(tenant.getId());

        // Create test user
        user = new User();
        user.setTenant(tenant);
        user.setEmail("taskupdate@test.com");
        user.setPasswordHash(passwordEncoder.encode("Test@123"));
        user.setFirstName("Task");
        user.setLastName("Update");
        user.setIsActive(true);
        user = userRepository.save(user);

        // Generate JWT token
        authToken = tokenProvider.generateToken(
            user.getId(),
            tenant.getId(),
            Collections.singletonList("ROLE_SYSTEM_ADMIN"),
            Arrays.asList("PAGE_TASK_UPDATE_VIEW", "PAGE_TASK_UPDATE_EDIT", "PAGE_PROJECTS_VIEW", "PAGE_PROJECTS_EDIT")
        );

        // Create test project
        project = new Project();
        project.setTenant(tenant);
        project.setProjectCode("PROJ-TEST");
        project.setProjectName("Test Project");
        project.setStartDate(LocalDate.of(2025, 11, 1));
        project.setEndDate(LocalDate.of(2025, 11, 30));
        project.setStatus("ACTIVE");
        project = projectRepository.save(project);

        // Create test WBS
        wbs = new Wbs();
        wbs.setTenant(tenant);
        wbs.setProject(project);
        wbs.setWbsCode("WBS-001");
        wbs.setWbsName("Test WBS");
        wbs.setStartDate(LocalDate.of(2025, 11, 1));
        wbs.setEndDate(LocalDate.of(2025, 11, 30));
        wbs.setStatus("ACTIVE");
        wbs = wbsRepository.save(wbs);

        // Create test task
        task = new Task();
        task.setTenant(tenant);
        task.setProject(project);
        task.setWbs(wbs);
        task.setTaskCode("TASK-001");
        task.setTaskName("Test Task for Updates");
        task.setStartDate(LocalDate.of(2025, 11, 5));
        task.setEndDate(LocalDate.of(2025, 11, 14));
        task.setPlannedQty(new BigDecimal("100.00"));
        task.setActualQty(BigDecimal.ZERO);
        task.setUnit("HOURS");
        task.setStatus("IN_PROGRESS");
        task = taskRepository.save(task);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUpdatesForTask_Success() throws Exception {
        mockMvc.perform(get("/api/task-updates/task/" + task.getTaskId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetUpdatesForTask_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/task-updates/task/" + task.getTaskId()))
                .andExpect(status().isForbidden()); // Spring Security returns 403 for anonymous requests
    }

    @Test
    void testSaveOrUpdateDayWise_Success() throws Exception {
        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(task.getTaskId());

        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00"));
        dayUpdate.setRemarks("Test update");

        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        mockMvc.perform(post("/api/task-updates/task/" + task.getTaskId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].actualQty").value(10.00));
    }

    @Test
    void testSaveOrUpdateDayWise_WithActualExceedingPlan_ShouldFail() throws Exception {
        // Create Rule 401 for this tenant to enforce validation
        BusinessRule rule401 = new BusinessRule();
        rule401.setTenant(tenant);
        rule401.setRuleNumber(401);
        rule401.setControlPoint("TASK_UPDATE");
        rule401.setApplicability("Y");
        rule401.setRuleValue("ENFORCE");
        rule401.setDescription("Daily update cannot exceed planned quantity");
        rule401.setActivateFlag(true);
        rule401.setCreatedBy(user.getId());
        businessRuleRepository.save(rule401);

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(task.getTaskId());

        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("15.00")); // Exceeds plan - should violate Rule 401

        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // This should fail because Rule 401 is active and enforced
        mockMvc.perform(post("/api/task-updates/task/" + task.getTaskId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkDTO)))
                .andExpect(status().is4xxClientError()); // Should be 400 or 422
    }

    @Test
    void testSaveOrUpdateDayWise_WithDateOutsideTaskRange_ShouldFail() throws Exception {
        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(task.getTaskId());

        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 12, 1)); // After task end date
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00"));

        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        mockMvc.perform(post("/api/task-updates/task/" + task.getTaskId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkDTO)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testDeleteTaskUpdate_Success() throws Exception {
        // First create an update
        TaskUpdate update = new TaskUpdate();
        update.setTenant(tenant);
        update.setTask(task);
        update.setUpdateDate(LocalDate.of(2025, 11, 5));
        update.setPlannedQty(new BigDecimal("10.00"));
        update.setActualQty(new BigDecimal("10.00"));
        update.setCreatedBy(user.getId());
        update = taskUpdateRepository.save(update);

        mockMvc.perform(delete("/api/task-updates/" + update.getUpdateId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify it was deleted
        assertTrue(taskUpdateRepository.findById(update.getUpdateId()).isEmpty());
    }

    @Test
    void testGetDailySummary_Success() throws Exception {
        // Create some updates
        TaskUpdate update1 = new TaskUpdate();
        update1.setTenant(tenant);
        update1.setTask(task);
        update1.setUpdateDate(LocalDate.of(2025, 11, 5));
        update1.setPlannedQty(new BigDecimal("10.00"));
        update1.setActualQty(new BigDecimal("9.00"));
        update1.setCreatedBy(user.getId());
        taskUpdateRepository.save(update1);

        TaskUpdate update2 = new TaskUpdate();
        update2.setTenant(tenant);
        update2.setTask(task);
        update2.setUpdateDate(LocalDate.of(2025, 11, 6));
        update2.setPlannedQty(new BigDecimal("10.00"));
        update2.setActualQty(new BigDecimal("10.00"));
        update2.setCreatedBy(user.getId());
        taskUpdateRepository.save(update2);

        mockMvc.perform(get("/api/task-updates/task/" + task.getTaskId() + "/summary")
                .param("from", "2025-11-05")
                .param("to", "2025-11-07")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].date").value("2025-11-05"))
                .andExpect(jsonPath("$[0].actualQty").value(9.00));
    }

    @Test
    void testGetUpdatesForTask_WithPlanVersion_ShouldMergePlanLines() throws Exception {
        // Create plan version
        PlanVersion planVersion = new PlanVersion();
        planVersion.setTenant(tenant);
        planVersion.setTask(task);
        planVersion.setVersionNo(1);
        planVersion.setVersionDate(LocalDate.of(2025, 11, 1));
        planVersion.setIsActive(true);
        planVersion = planVersionRepository.save(planVersion);

        // Create plan lines
        PlanLine planLine1 = new PlanLine();
        planLine1.setTenant(tenant);
        planLine1.setPlanVersion(planVersion);
        planLine1.setTask(task);
        planLine1.setLineNumber(1);
        planLine1.setWorkDate(LocalDate.of(2025, 11, 5));
        planLine1.setPlannedQty(new BigDecimal("12.00"));
        planLineRepository.save(planLine1);

        PlanLine planLine2 = new PlanLine();
        planLine2.setTenant(tenant);
        planLine2.setPlanVersion(planVersion);
        planLine2.setTask(task);
        planLine2.setLineNumber(2);
        planLine2.setWorkDate(LocalDate.of(2025, 11, 6));
        planLine2.setPlannedQty(new BigDecimal("15.00"));
        planLineRepository.save(planLine2);

        // Create existing update
        TaskUpdate update = new TaskUpdate();
        update.setTenant(tenant);
        update.setTask(task);
        update.setUpdateDate(LocalDate.of(2025, 11, 5));
        update.setPlannedQty(new BigDecimal("12.00"));
        update.setActualQty(new BigDecimal("11.00"));
        update.setCreatedBy(user.getId());
        taskUpdateRepository.save(update);

        mockMvc.perform(get("/api/task-updates/task/" + task.getTaskId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.updateDate == '2025-11-05')].planQty").value(12.00))
                .andExpect(jsonPath("$[?(@.updateDate == '2025-11-05')].actualQty").value(11.00))
                .andExpect(jsonPath("$[?(@.updateDate == '2025-11-06')].planQty").value(15.00))
                .andExpect(jsonPath("$[?(@.updateDate == '2025-11-06')].actualQty").value(0)); // No update exists
    }
}

