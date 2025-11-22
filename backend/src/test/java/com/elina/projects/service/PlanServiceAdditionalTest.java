package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.*;
import com.elina.projects.entity.PlanLine;
import com.elina.projects.entity.PlanVersion;
import com.elina.projects.entity.Task;
import com.elina.projects.entity.Wbs;
import com.elina.projects.exception.NotFoundException;
import com.elina.projects.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for PlanService.
 * Tests edge cases, business rule violations, and delete operations.
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceAdditionalTest {

    @Mock
    private PlanVersionRepository planVersionRepository;

    @Mock
    private PlanLineRepository planLineRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ConfirmationLockRepository confirmationLockRepository;

    @Mock
    private BusinessRuleEngine businessRuleEngine;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PlanService planService;

    private Tenant tenant;
    private Task task;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("DEFAULT");
        tenant.setName("Default Tenant");

        task = new Task();
        task.setTaskId(1L);
        task.setTenant(tenant);
        task.setTaskCode("TASK001");
        task.setTaskName("Test Task");
        task.setStartDate(LocalDate.of(2025, 1, 1));
        task.setEndDate(LocalDate.of(2025, 1, 31));
        task.setActivateFlag(true);

        Wbs wbs = new Wbs();
        wbs.setWbsId(1L);
        wbs.setTenant(tenant);
        task.setWbs(wbs);

        SecurityContextHolder.setContext(securityContext);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testListPlanVersions_Success() {
        // Arrange
        PlanVersion version1 = new PlanVersion();
        version1.setPlanVersionId(1L);
        version1.setTenant(tenant);
        version1.setTask(task);
        version1.setVersionNo(1);
        version1.setIsActive(true);

        PlanVersion version2 = new PlanVersion();
        version2.setPlanVersionId(2L);
        version2.setTenant(tenant);
        version2.setTask(task);
        version2.setVersionNo(2);
        version2.setIsActive(false);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Arrays.asList(version2, version1));

        // Act
        List<PlanVersionDTO> result = planService.listPlanVersions(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void testListPlanVersions_TaskNotFound_ThrowsException() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> planService.listPlanVersions(1L));
    }

    @Test
    void testGetPlanVersion_Success() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(1);
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));

        // Act
        PlanVersionDTO result = planService.getPlanVersion(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getVersionNo());
    }

    @Test
    void testGetPlanVersion_NotFound_ThrowsException() {
        // Arrange
        when(planVersionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> planService.getPlanVersion(1L));
    }

    @Test
    void testGetPlanLines_Success() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));
        version.setVersionDate(LocalDate.of(2025, 1, 5));

        PlanLine line1 = new PlanLine();
        line1.setPlanLineId(1L);
        line1.setWorkDate(LocalDate.of(2025, 1, 10));
        line1.setPlannedQty(BigDecimal.valueOf(100.0));
        line1.setTenant(tenant);
        line1.setPlanVersion(version);
        line1.setTask(task);

        PlanLine line2 = new PlanLine();
        line2.setPlanLineId(2L);
        line2.setWorkDate(LocalDate.of(2025, 1, 15));
        line2.setPlannedQty(BigDecimal.valueOf(150.0));
        line2.setTenant(tenant);
        line2.setPlanVersion(version);
        line2.setTask(task);

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(planLineRepository.findByPlanVersionId(1L, true)).thenReturn(Arrays.asList(line1, line2));

        // Act
        List<PlanLineDTO> result = planService.getPlanLines(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testSetActiveVersion_Success() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(1);
        version.setIsActive(false);

        PlanVersion oldActive = new PlanVersion();
        oldActive.setPlanVersionId(2L);
        oldActive.setTenant(tenant);
        oldActive.setTask(task);
        oldActive.setVersionNo(2);
        oldActive.setIsActive(true);

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(planVersionRepository.findByTaskId(task.getTaskId(), true)).thenReturn(Arrays.asList(oldActive, version));
        when(planVersionRepository.save(any(PlanVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PlanVersionDTO result = planService.setActiveVersion(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(planVersionRepository, atLeastOnce()).save(any(PlanVersion.class));
    }

    @Test
    void testDeletePlanVersion_Success() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(1);
        version.setVersionDate(LocalDate.of(2025, 1, 5));

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(confirmationLockRepository.findLockDateByWbsId(task.getWbs().getWbsId())).thenReturn(null);
        when(planLineRepository.findByPlanVersionId(1L, null)).thenReturn(Collections.emptyList());

        // Act
        assertDoesNotThrow(() -> planService.deletePlanVersion(1L));

        // Assert
        verify(planVersionRepository, times(1)).delete(version);
        verify(auditLogService, times(1)).writeAuditLog(eq("PLAN_VERSION"), eq(1L), eq("DELETE"), any(), isNull());
    }

    @Test
    void testDeletePlanVersion_TaskConfirmed_ThrowsException() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionDate(LocalDate.of(2025, 1, 5));

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(confirmationLockRepository.findLockDateByWbsId(task.getWbs().getWbsId()))
            .thenReturn(LocalDate.of(2025, 1, 15));

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> planService.deletePlanVersion(1L));
        assertEquals(102, exception.getRuleNumber());
    }

    @Test
    void testCreateOrUpdatePlanLines_UpdateExisting_Success() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);

        PlanLine existingLine = new PlanLine();
        existingLine.setPlanLineId(1L);
        existingLine.setWorkDate(LocalDate.of(2025, 1, 10));
        existingLine.setPlannedQty(BigDecimal.valueOf(100.0));

        PlanLineCreateDTO lineDto = new PlanLineCreateDTO();
        lineDto.setWorkDate(LocalDate.of(2025, 1, 10));
        lineDto.setPlannedQty(BigDecimal.valueOf(150.0));
        lineDto.setLineNumber(1);

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(planLineRepository.findByPlanVersionId(1L, null)).thenReturn(Arrays.asList(existingLine));
        when(planLineRepository.save(any(PlanLine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PlanVersionDTO result = planService.createOrUpdatePlanLines(1L, Arrays.asList(lineDto), null);

        // Assert
        assertNotNull(result);
        verify(planLineRepository, times(1)).save(any(PlanLine.class));
    }

    @Test
    void testCreateOrUpdatePlanLines_DateOutsideTaskRange_ThrowsException() {
        // Arrange
        PlanVersion version = new PlanVersion();
        version.setPlanVersionId(1L);
        version.setTenant(tenant);
        version.setTask(task);

        PlanLineCreateDTO lineDto = new PlanLineCreateDTO();
        lineDto.setWorkDate(LocalDate.of(2026, 1, 10)); // Outside task range
        lineDto.setPlannedQty(BigDecimal.valueOf(100.0));
        lineDto.setLineNumber(1);

        when(planVersionRepository.findById(1L)).thenReturn(Optional.of(version));

        // Act & Assert
        assertThrows(BusinessRuleException.class, 
            () -> planService.createOrUpdatePlanLines(1L, Arrays.asList(lineDto), null));
    }

    @Test
    void testCreatePlanVersion_DateRangeSplit_MonthlySplit_Success() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DATE_RANGE_SPLIT);

        PlanCreationModeDTO.DateRangeSplitDTO rangeSplit = new PlanCreationModeDTO.DateRangeSplitDTO();
        rangeSplit.setStartDate(LocalDate.of(2025, 1, 1));
        rangeSplit.setEndDate(LocalDate.of(2025, 1, 31));
        rangeSplit.setTotalQty(BigDecimal.valueOf(310.0));
        rangeSplit.setSplitType(PlanCreationModeDTO.DateRangeSplitDTO.SplitType.MONTHLY_SPLIT);
        dto.setRangeSplit(rangeSplit);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(planVersionRepository.findMaxVersionNoByTaskId(1L)).thenReturn(null);
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Collections.emptyList());
        when(planVersionRepository.save(any(PlanVersion.class))).thenAnswer(invocation -> {
            PlanVersion v = invocation.getArgument(0);
            v.setPlanVersionId(1L);
            return v;
        });
        when(planLineRepository.save(any(PlanLine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);

        // Assert
        assertNotNull(result);
        verify(planVersionRepository, times(1)).save(any(PlanVersion.class));
        verify(planLineRepository, atLeastOnce()).save(any(PlanLine.class));
    }
}

