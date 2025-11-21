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
 * Unit tests for PlanService.
 * Tests all 3 creation modes, version comparison, and business rule validation.
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanVersionRepository planVersionRepository;

    @Mock
    private PlanLineRepository planLineRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ConfirmationRepository confirmationRepository;

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
    private PlanVersion planVersion;
    private PlanLine planLine;

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

        planVersion = new PlanVersion();
        planVersion.setPlanVersionId(1L);
        planVersion.setTenant(tenant);
        planVersion.setTask(task);
        planVersion.setVersionNo(1);
        planVersion.setVersionDate(LocalDate.now());
        planVersion.setIsActive(true);
        planVersion.setActivateFlag(true);
        planVersion.setCreatedBy(1L);

        planLine = new PlanLine();
        planLine.setPlanLineId(1L);
        planLine.setTenant(tenant);
        planLine.setPlanVersion(planVersion);
        planLine.setTask(task);
        planLine.setLineNumber(1);
        planLine.setWorkDate(LocalDate.of(2025, 1, 15));
        planLine.setPlannedQty(BigDecimal.valueOf(100.0));
        planLine.setActivateFlag(true);

        // Mock SecurityContext - use lenient to avoid unnecessary stubbing errors
        SecurityContextHolder.setContext(securityContext);
        // Use lenient() for mocks that may not be used in all tests
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ========== CREATE PLAN VERSION - DAILY ENTRY MODE ==========

    @Test
    void testCreatePlanVersionWithMode_DailyEntry_Success() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DAILY_ENTRY);
        dto.setDescription("Test Daily Entry");

        List<PlanCreationModeDTO.DailyPlanLineDTO> dailyLines = new ArrayList<>();
        PlanCreationModeDTO.DailyPlanLineDTO line1 = new PlanCreationModeDTO.DailyPlanLineDTO();
        line1.setPlannedDate(LocalDate.of(2025, 1, 10));
        line1.setPlannedQty(BigDecimal.valueOf(50.0));
        dailyLines.add(line1);
        dto.setDailyLines(dailyLines);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(planVersionRepository.findMaxVersionNoByTaskId(1L)).thenReturn(null);
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Collections.emptyList());
        when(planVersionRepository.save(any(PlanVersion.class))).thenReturn(planVersion);
        when(planLineRepository.save(any(PlanLine.class))).thenReturn(planLine);

        // Act
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getVersionNo());
        verify(planVersionRepository, times(1)).save(any(PlanVersion.class));
        verify(planLineRepository, times(1)).save(any(PlanLine.class));
        verify(businessRuleEngine, atLeastOnce()).validate(anyInt(), any(BusinessRuleContext.class));
    }

    @Test
    void testCreatePlanVersionWithMode_DailyEntry_EmptyLines_ThrowsException() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DAILY_ENTRY);
        dto.setDailyLines(Collections.emptyList());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> planService.createPlanVersionWithMode(dto));
    }

    @Test
    void testCreatePlanVersionWithMode_DailyEntry_DateOutOfRange_ThrowsException() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DAILY_ENTRY);

        List<PlanCreationModeDTO.DailyPlanLineDTO> dailyLines = new ArrayList<>();
        PlanCreationModeDTO.DailyPlanLineDTO line1 = new PlanCreationModeDTO.DailyPlanLineDTO();
        line1.setPlannedDate(LocalDate.of(2026, 1, 10)); // Outside task date range
        line1.setPlannedQty(BigDecimal.valueOf(50.0));
        dailyLines.add(line1);
        dto.setDailyLines(dailyLines);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> planService.createPlanVersionWithMode(dto));
    }

    // ========== CREATE PLAN VERSION - DATE RANGE SPLIT MODE ==========

    @Test
    void testCreatePlanVersionWithMode_DateRangeSplit_EqualSplit_Success() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DATE_RANGE_SPLIT);

        PlanCreationModeDTO.DateRangeSplitDTO rangeSplit = new PlanCreationModeDTO.DateRangeSplitDTO();
        rangeSplit.setStartDate(LocalDate.of(2025, 1, 1));
        rangeSplit.setEndDate(LocalDate.of(2025, 1, 10));
        rangeSplit.setTotalQty(BigDecimal.valueOf(100.0));
        rangeSplit.setSplitType(PlanCreationModeDTO.DateRangeSplitDTO.SplitType.EQUAL_SPLIT);
        dto.setRangeSplit(rangeSplit);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(planVersionRepository.findMaxVersionNoByTaskId(1L)).thenReturn(null);
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Collections.emptyList());
        when(planVersionRepository.save(any(PlanVersion.class))).thenReturn(planVersion);
        when(planLineRepository.save(any(PlanLine.class))).thenReturn(planLine);

        // Act
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);

        // Assert
        assertNotNull(result);
        verify(planVersionRepository, times(1)).save(any(PlanVersion.class));
        verify(planLineRepository, atLeastOnce()).save(any(PlanLine.class));
    }

    @Test
    void testCreatePlanVersionWithMode_DateRangeSplit_WeeklySplit_Success() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DATE_RANGE_SPLIT);

        PlanCreationModeDTO.DateRangeSplitDTO rangeSplit = new PlanCreationModeDTO.DateRangeSplitDTO();
        rangeSplit.setStartDate(LocalDate.of(2025, 1, 1));
        rangeSplit.setEndDate(LocalDate.of(2025, 1, 14));
        rangeSplit.setTotalQty(BigDecimal.valueOf(140.0));
        rangeSplit.setSplitType(PlanCreationModeDTO.DateRangeSplitDTO.SplitType.WEEKLY_SPLIT);
        dto.setRangeSplit(rangeSplit);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(planVersionRepository.findMaxVersionNoByTaskId(1L)).thenReturn(null);
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Collections.emptyList());
        when(planVersionRepository.save(any(PlanVersion.class))).thenReturn(planVersion);
        when(planLineRepository.save(any(PlanLine.class))).thenReturn(planLine);

        // Act
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);

        // Assert
        assertNotNull(result);
        verify(planVersionRepository, times(1)).save(any(PlanVersion.class));
        verify(planLineRepository, atLeastOnce()).save(any(PlanLine.class));
    }

    @Test
    void testCreatePlanVersionWithMode_DateRangeSplit_CustomSplit_Success() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DATE_RANGE_SPLIT);

        PlanCreationModeDTO.DateRangeSplitDTO rangeSplit = new PlanCreationModeDTO.DateRangeSplitDTO();
        rangeSplit.setStartDate(LocalDate.of(2025, 1, 1));
        rangeSplit.setEndDate(LocalDate.of(2025, 1, 10));
        rangeSplit.setTotalQty(BigDecimal.valueOf(100.0));
        rangeSplit.setSplitType(PlanCreationModeDTO.DateRangeSplitDTO.SplitType.CUSTOM_SPLIT);
        rangeSplit.setSplitCount(2);
        rangeSplit.setCustomQuantities(Arrays.asList(BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0)));
        dto.setRangeSplit(rangeSplit);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(planVersionRepository.findMaxVersionNoByTaskId(1L)).thenReturn(null);
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Collections.emptyList());
        when(planVersionRepository.save(any(PlanVersion.class))).thenReturn(planVersion);
        when(planLineRepository.save(any(PlanLine.class))).thenReturn(planLine);

        // Act
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);

        // Assert
        assertNotNull(result);
        verify(planVersionRepository, times(1)).save(any(PlanVersion.class));
        verify(planLineRepository, atLeastOnce()).save(any(PlanLine.class));
    }

    @Test
    void testCreatePlanVersionWithMode_DateRangeSplit_InvalidRange_ThrowsException() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.DATE_RANGE_SPLIT);

        PlanCreationModeDTO.DateRangeSplitDTO rangeSplit = new PlanCreationModeDTO.DateRangeSplitDTO();
        rangeSplit.setStartDate(LocalDate.of(2025, 1, 10));
        rangeSplit.setEndDate(LocalDate.of(2025, 1, 1)); // End before start
        rangeSplit.setTotalQty(BigDecimal.valueOf(100.0));
        rangeSplit.setSplitType(PlanCreationModeDTO.DateRangeSplitDTO.SplitType.EQUAL_SPLIT);
        dto.setRangeSplit(rangeSplit);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> planService.createPlanVersionWithMode(dto));
    }

    // ========== CREATE PLAN VERSION - SINGLE LINE QUICK MODE ==========

    @Test
    void testCreatePlanVersionWithMode_SingleLineQuick_Success() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.SINGLE_LINE_QUICK);

        PlanCreationModeDTO.SingleLineQuickDTO singleLine = new PlanCreationModeDTO.SingleLineQuickDTO();
        singleLine.setPlannedDate(LocalDate.of(2025, 1, 15));
        singleLine.setPlannedQty(BigDecimal.valueOf(100.0));
        singleLine.setDescription("Quick plan");
        dto.setSingleLine(singleLine);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(planVersionRepository.findMaxVersionNoByTaskId(1L)).thenReturn(null);
        when(planVersionRepository.findByTaskId(1L, true)).thenReturn(Collections.emptyList());
        when(planVersionRepository.save(any(PlanVersion.class))).thenReturn(planVersion);
        when(planLineRepository.save(any(PlanLine.class))).thenReturn(planLine);

        // Act
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getVersionNo());
        verify(planVersionRepository, times(1)).save(any(PlanVersion.class));
        verify(planLineRepository, times(1)).save(any(PlanLine.class));
    }

    @Test
    void testCreatePlanVersionWithMode_SingleLineQuick_MissingFields_ThrowsException() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.SINGLE_LINE_QUICK);
        dto.setSingleLine(null); // Missing single line

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> planService.createPlanVersionWithMode(dto));
    }

    // ========== VERSION COMPARISON ==========

    @Test
    void testComparePlanVersions_Success() {
        // Arrange
        Long versionId1 = 1L;
        Long versionId2 = 2L;

        PlanVersion version1 = new PlanVersion();
        version1.setPlanVersionId(versionId1);
        version1.setTenant(tenant);
        version1.setTask(task);
        version1.setVersionNo(1);
        version1.setVersionDate(LocalDate.of(2025, 1, 1));

        PlanVersion version2 = new PlanVersion();
        version2.setPlanVersionId(versionId2);
        version2.setTenant(tenant);
        version2.setTask(task);
        version2.setVersionNo(2);
        version2.setVersionDate(LocalDate.of(2025, 1, 2));

        PlanLine line1 = new PlanLine();
        line1.setWorkDate(LocalDate.of(2025, 1, 10));
        line1.setPlannedQty(BigDecimal.valueOf(100.0));

        PlanLine line2 = new PlanLine();
        line2.setWorkDate(LocalDate.of(2025, 1, 10));
        line2.setPlannedQty(BigDecimal.valueOf(150.0));

        PlanLine line3 = new PlanLine();
        line3.setWorkDate(LocalDate.of(2025, 1, 15));
        line3.setPlannedQty(BigDecimal.valueOf(50.0));

        when(planVersionRepository.findById(versionId1)).thenReturn(Optional.of(version1));
        when(planVersionRepository.findById(versionId2)).thenReturn(Optional.of(version2));
        when(planLineRepository.findByPlanVersionId(versionId1, true)).thenReturn(Arrays.asList(line1));
        when(planLineRepository.findByPlanVersionId(versionId2, true)).thenReturn(Arrays.asList(line2, line3));

        // Act
        PlanVersionComparisonDTO result = planService.comparePlanVersions(versionId1, versionId2);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getVersion1());
        assertNotNull(result.getVersion2());
        assertNotNull(result.getComparisonLines());
        assertNotNull(result.getSummary());
        assertEquals(1, result.getVersion1().getVersionNo());
        assertEquals(2, result.getVersion2().getVersionNo());
    }

    @Test
    void testComparePlanVersions_VersionNotFound_ThrowsException() {
        // Arrange
        Long versionId1 = 1L;
        Long versionId2 = 2L;

        when(planVersionRepository.findById(versionId1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> planService.comparePlanVersions(versionId1, versionId2));
    }

    // ========== BUSINESS RULE VALIDATION ==========

    @Test
    void testCreatePlanVersionWithMode_BusinessRuleViolation_ThrowsException() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(1L);
        dto.setVersionDate(LocalDate.now());
        dto.setMode(PlanCreationModeDTO.CreationMode.SINGLE_LINE_QUICK);

        PlanCreationModeDTO.SingleLineQuickDTO singleLine = new PlanCreationModeDTO.SingleLineQuickDTO();
        singleLine.setPlannedDate(LocalDate.of(2025, 1, 15));
        singleLine.setPlannedQty(BigDecimal.valueOf(100.0));
        dto.setSingleLine(singleLine);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doThrow(new BusinessRuleException(101, "Backdate not allowed", "Hint"))
                .when(businessRuleEngine).validate(eq(101), any(BusinessRuleContext.class));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> planService.createPlanVersionWithMode(dto));
    }

    // ========== NOT FOUND EXCEPTIONS ==========

    @Test
    void testCreatePlanVersionWithMode_TaskNotFound_ThrowsNotFoundException() {
        // Arrange
        PlanCreationModeDTO dto = new PlanCreationModeDTO();
        dto.setTaskId(999L);
        dto.setMode(PlanCreationModeDTO.CreationMode.SINGLE_LINE_QUICK);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> planService.createPlanVersionWithMode(dto));
    }

    @Test
    void testComparePlanVersions_TenantMismatch_ThrowsNotFoundException() {
        // Arrange
        Long versionId1 = 1L;
        Long versionId2 = 2L;

        Tenant otherTenant = new Tenant();
        otherTenant.setId(2L);

        PlanVersion version1 = new PlanVersion();
        version1.setPlanVersionId(versionId1);
        version1.setTenant(otherTenant); // Different tenant

        when(planVersionRepository.findById(versionId1)).thenReturn(Optional.of(version1));

        // Act & Assert
        assertThrows(NotFoundException.class, () -> planService.comparePlanVersions(versionId1, versionId2));
    }
}

