package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.TaskUpdateBulkDTO;
import com.elina.projects.dto.TaskUpdateDayWiseDTO;
import com.elina.projects.entity.*;
import com.elina.projects.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for TaskUpdateService.
 * 
 * Tests cover:
 * - Rule 101 violation (backdate too old)
 * - Rule 102 violation (update on locked date)
 * - Rule 401 violation (actual > plan)
 * - Date range validation (outside task range)
 * - Confirmation lock blocking
 * - Successful save operations
 */
@ExtendWith(MockitoExtension.class)
class TaskUpdateServiceTest {

    @Mock
    private TaskUpdateRepository taskUpdateRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PlanLineRepository planLineRepository;

    @Mock
    private PlanVersionRepository planVersionRepository;

    @Mock
    private ConfirmationRepository confirmationRepository;

    @Mock
    private BusinessRuleEngine businessRuleEngine;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaskUpdateService taskUpdateService;

    private Task task;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        // Set tenant context
        TenantContext.setTenantId(1L);

        // Create test tenant
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("TEST_TENANT");

        // Create test task
        task = new Task();
        task.setTaskId(1L);
        task.setTenant(tenant);
        task.setTaskCode("TASK-001");
        task.setTaskName("Test Task");
        task.setStartDate(LocalDate.of(2025, 11, 1));
        task.setEndDate(LocalDate.of(2025, 11, 30));
        task.setPlannedQty(new BigDecimal("100.00"));
        task.setActualQty(BigDecimal.ZERO);
        task.setIsConfirmed(false);
        task.setIsLocked(false);
    }

    @Test
    void testSaveOrUpdateDayWise_WithRule401Violation_ShouldThrowException() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("15.00")); // Exceeds plan - should violate Rule 401
        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // Mock business rule engine to throw Rule 401 violation
        doThrow(new BusinessRuleException(401, "Actual quantity cannot exceed planned quantity"))
                .when(businessRuleEngine).validateAll(anyList(), any());

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            taskUpdateService.saveOrUpdateDayWise(bulkDTO);
        });

        // Verify business rule engine was called
        verify(businessRuleEngine, times(1)).validateAll(anyList(), any());
        // Verify no save occurred
        verify(taskUpdateRepository, never()).save(any());
    }

    @Test
    void testSaveOrUpdateDayWise_WithDateOutsideTaskRange_ShouldThrowException() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 12, 1)); // After task end date
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00"));
        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            taskUpdateService.saveOrUpdateDayWise(bulkDTO);
        });

        // Verify no save occurred
        verify(taskUpdateRepository, never()).save(any());
    }

    @Test
    void testSaveOrUpdateDayWise_WithLockedDate_ShouldThrowException() {
        // Arrange
        Wbs wbs = new Wbs();
        wbs.setWbsId(1L);
        task.setWbs(wbs);
        
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Create confirmation lock
        Confirmation confirmation = new Confirmation();
        confirmation.setConfirmationId(1L);
        confirmation.setEntityType("WBS");
        confirmation.setEntityId(1L);
        confirmation.setConfirmationDate(LocalDate.of(2025, 11, 10)); // Locks date 2025-11-10 and earlier

        when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), eq(1L))).thenReturn(Optional.of(confirmation));

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 11, 5)); // Before lock date
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00"));
        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // Mock Rule 102 to throw exception (backdate not allowed after lock)
        doThrow(new BusinessRuleException(102, "Cannot update locked date"))
                .when(businessRuleEngine).validate(eq(102), any());

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            taskUpdateService.saveOrUpdateDayWise(bulkDTO);
        });

        // Verify Rule 102 was checked
        verify(businessRuleEngine, atLeastOnce()).validate(eq(102), any());
    }

    @Test
    void testSaveOrUpdateDayWise_WithValidData_ShouldSaveSuccessfully() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());
        when(taskUpdateRepository.findByTaskIdAndUpdateDate(anyLong(), any())).thenReturn(Optional.empty());
        when(taskUpdateRepository.getTotalActualQtyByTaskId(anyLong())).thenReturn(new BigDecimal("10.00"));

        TaskUpdate savedUpdate = new TaskUpdate();
        savedUpdate.setUpdateId(1L);
        savedUpdate.setTask(task);
        savedUpdate.setTenant(tenant);
        savedUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        savedUpdate.setActualQty(new BigDecimal("10.00"));
        when(taskUpdateRepository.save(any())).thenReturn(savedUpdate);

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00"));
        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // Act
        List<?> result = taskUpdateService.saveOrUpdateDayWise(bulkDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskUpdateRepository, times(1)).save(any());
        verify(auditLogService, times(1)).writeAuditLog(anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void testDeleteTaskUpdate_WithLockedDate_ShouldThrowException() {
        // Arrange
        Wbs wbs = new Wbs();
        wbs.setWbsId(1L);
        task.setWbs(wbs);
        
        TaskUpdate update = new TaskUpdate();
        update.setUpdateId(1L);
        update.setTask(task);
        update.setTenant(tenant);
        update.setUpdateDate(LocalDate.of(2025, 11, 5));

        when(taskUpdateRepository.findById(1L)).thenReturn(Optional.of(update));

        // Create confirmation lock
        Confirmation confirmation = new Confirmation();
        confirmation.setConfirmationDate(LocalDate.of(2025, 11, 10));
        when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), eq(1L))).thenReturn(Optional.of(confirmation));

        // Mock Rule 102 to throw exception
        doThrow(new BusinessRuleException(102, "Cannot delete update for locked date"))
                .when(businessRuleEngine).validate(eq(102), any());

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            taskUpdateService.deleteTaskUpdate(1L);
        });

        // Verify delete was not called
        verify(taskUpdateRepository, never()).delete(any(TaskUpdate.class));
    }

    @Test
    void testSaveOrUpdateDayWise_WithRule101Violation_ShouldThrowException() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 10, 1)); // Before task start date
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00"));
        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            taskUpdateService.saveOrUpdateDayWise(bulkDTO);
        });

        // Verify no save occurred
        verify(taskUpdateRepository, never()).save(any());
    }

    @Test
    void testGetUpdatesForTask_WithPlanVersion_ShouldMergePlanLines() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Create plan version
        PlanVersion planVersion = new PlanVersion();
        planVersion.setPlanVersionId(1L);
        planVersion.setTask(task);
        planVersion.setIsActive(true);
        when(planVersionRepository.findCurrentByTaskId(1L)).thenReturn(Optional.of(planVersion));

        // Create plan lines
        PlanLine planLine1 = new PlanLine();
        planLine1.setWorkDate(LocalDate.of(2025, 11, 5));
        planLine1.setPlannedQty(new BigDecimal("10.00"));
        
        PlanLine planLine2 = new PlanLine();
        planLine2.setWorkDate(LocalDate.of(2025, 11, 6));
        planLine2.setPlannedQty(new BigDecimal("12.00"));

        when(planLineRepository.findByPlanVersionId(1L, true))
                .thenReturn(Arrays.asList(planLine1, planLine2));

        // Create existing update
        TaskUpdate existingUpdate = new TaskUpdate();
        existingUpdate.setUpdateId(1L);
        existingUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        existingUpdate.setActualQty(new BigDecimal("9.00"));

        when(taskUpdateRepository.findByTaskIdOrderByUpdateDate(1L, true))
                .thenReturn(Arrays.asList(existingUpdate));

        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        // Act
        List<TaskUpdateDayWiseDTO> result = taskUpdateService.getUpdatesForTask(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 2); // At least 2 days in range
        // Verify plan lines were merged
        verify(planLineRepository, times(1)).findByPlanVersionId(1L, true);
    }

    @Test
    void testGetUpdatesForTask_WithoutPlanVersion_ShouldUseTaskPlannedQty() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(planVersionRepository.findCurrentByTaskId(1L)).thenReturn(Optional.empty());
        when(taskUpdateRepository.findByTaskIdOrderByUpdateDate(1L, true))
                .thenReturn(Collections.emptyList());
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        // Act
        List<TaskUpdateDayWiseDTO> result = taskUpdateService.getUpdatesForTask(1L);

        // Assert
        assertNotNull(result);
        // Should generate entries for all dates in task range
        assertTrue(result.size() > 0);
    }

    @Test
    void testGetDailySummary_ShouldReturnSummaryForDateRange() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        LocalDate fromDate = LocalDate.of(2025, 11, 5);
        LocalDate toDate = LocalDate.of(2025, 11, 7);

        // Create plan version
        PlanVersion planVersion = new PlanVersion();
        planVersion.setPlanVersionId(1L);
        when(planVersionRepository.findCurrentByTaskId(1L)).thenReturn(Optional.of(planVersion));

        // Create plan lines
        PlanLine planLine = new PlanLine();
        planLine.setWorkDate(LocalDate.of(2025, 11, 5));
        planLine.setPlannedQty(new BigDecimal("10.00"));
        when(planLineRepository.findByPlanVersionId(1L, true))
                .thenReturn(Arrays.asList(planLine));

        // Create task updates
        TaskUpdate update = new TaskUpdate();
        update.setUpdateDate(LocalDate.of(2025, 11, 5));
        update.setActualQty(new BigDecimal("9.00"));
        when(taskUpdateRepository.findByTaskIdAndDateRange(1L, fromDate, toDate, true))
                .thenReturn(Arrays.asList(update));

        // Act
        List<?> result = taskUpdateService.getDailySummary(1L, fromDate, toDate);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // 3 days in range
    }

    @Test
    void testSaveOrUpdateDayWise_WithMultipleUpdates_ShouldSaveAll() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());
        when(taskUpdateRepository.findByTaskIdAndUpdateDate(anyLong(), any())).thenReturn(Optional.empty());
        when(taskUpdateRepository.getTotalActualQtyByTaskId(anyLong())).thenReturn(new BigDecimal("30.00"));

        TaskUpdate savedUpdate1 = new TaskUpdate();
        savedUpdate1.setUpdateId(1L);
        savedUpdate1.setTask(task);
        savedUpdate1.setTenant(tenant);
        savedUpdate1.setUpdateDate(LocalDate.of(2025, 11, 5));
        savedUpdate1.setActualQty(new BigDecimal("10.00"));

        TaskUpdate savedUpdate2 = new TaskUpdate();
        savedUpdate2.setUpdateId(2L);
        savedUpdate2.setTask(task);
        savedUpdate2.setTenant(tenant);
        savedUpdate2.setUpdateDate(LocalDate.of(2025, 11, 6));
        savedUpdate2.setActualQty(new BigDecimal("10.00"));

        when(taskUpdateRepository.save(any()))
                .thenReturn(savedUpdate1)
                .thenReturn(savedUpdate2);

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate1 = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate1.setUpdateDate(LocalDate.of(2025, 11, 5));
        dayUpdate1.setPlanQty(new BigDecimal("10.00"));
        dayUpdate1.setActualQty(new BigDecimal("10.00"));

        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate2 = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate2.setUpdateDate(LocalDate.of(2025, 11, 6));
        dayUpdate2.setPlanQty(new BigDecimal("10.00"));
        dayUpdate2.setActualQty(new BigDecimal("10.00"));

        bulkDTO.setUpdates(Arrays.asList(dayUpdate1, dayUpdate2));

        // Act
        List<?> result = taskUpdateService.saveOrUpdateDayWise(bulkDTO);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(taskUpdateRepository, times(2)).save(any());
        verify(auditLogService, times(2)).writeAuditLog(anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void testSaveOrUpdateDayWise_WithExistingUpdate_ShouldUpdate() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        TaskUpdate existingUpdate = new TaskUpdate();
        existingUpdate.setUpdateId(1L);
        existingUpdate.setTask(task);
        existingUpdate.setTenant(tenant);
        existingUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        existingUpdate.setActualQty(new BigDecimal("8.00"));

        when(taskUpdateRepository.findByTaskIdAndUpdateDate(1L, LocalDate.of(2025, 11, 5)))
                .thenReturn(Optional.of(existingUpdate));
        when(taskUpdateRepository.getTotalActualQtyByTaskId(anyLong())).thenReturn(new BigDecimal("10.00"));

        TaskUpdate savedUpdate = new TaskUpdate();
        savedUpdate.setUpdateId(1L);
        savedUpdate.setTask(task);
        savedUpdate.setTenant(tenant);
        savedUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        savedUpdate.setActualQty(new BigDecimal("10.00"));
        when(taskUpdateRepository.save(any())).thenReturn(savedUpdate);

        TaskUpdateBulkDTO bulkDTO = new TaskUpdateBulkDTO();
        bulkDTO.setTaskId(1L);
        
        TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate = new TaskUpdateBulkDTO.DayWiseUpdateDTO();
        dayUpdate.setUpdateDate(LocalDate.of(2025, 11, 5));
        dayUpdate.setPlanQty(new BigDecimal("10.00"));
        dayUpdate.setActualQty(new BigDecimal("10.00")); // Updating from 8.00 to 10.00
        bulkDTO.setUpdates(Arrays.asList(dayUpdate));

        // Act
        List<?> result = taskUpdateService.saveOrUpdateDayWise(bulkDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskUpdateRepository, times(1)).save(existingUpdate); // Should update existing
        verify(auditLogService, times(1)).writeAuditLog(eq("task_updates"), eq(1L), eq("UPDATE"), any(), any());
    }

    @Test
    void testDeleteTaskUpdate_WithRule101Violation_ShouldThrowException() {
        // Arrange
        TaskUpdate update = new TaskUpdate();
        update.setUpdateId(1L);
        update.setTask(task);
        update.setTenant(tenant);
        update.setUpdateDate(LocalDate.of(2025, 10, 1)); // Very old date

        when(taskUpdateRepository.findById(1L)).thenReturn(Optional.of(update));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());

        // Mock Rule 101 to throw exception (backdate too old)
        doThrow(new BusinessRuleException(101, "Cannot delete update - backdate too old"))
                .when(businessRuleEngine).validate(eq(101), any());

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            taskUpdateService.deleteTaskUpdate(1L);
        });

        // Verify delete was not called
        verify(taskUpdateRepository, never()).delete(any(TaskUpdate.class));
    }

    @Test
    void testDeleteTaskUpdate_WithValidData_ShouldDeleteSuccessfully() {
        // Arrange
        TaskUpdate update = new TaskUpdate();
        update.setUpdateId(1L);
        update.setTask(task);
        update.setTenant(tenant);
        update.setUpdateDate(LocalDate.of(2025, 11, 5));

        when(taskUpdateRepository.findById(1L)).thenReturn(Optional.of(update));
        lenient().when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), any())).thenReturn(Optional.empty());
        when(taskUpdateRepository.getTotalActualQtyByTaskId(anyLong())).thenReturn(BigDecimal.ZERO);

        // Act
        taskUpdateService.deleteTaskUpdate(1L);

        // Assert
        verify(taskUpdateRepository, times(1)).delete(update);
        verify(auditLogService, times(1)).writeAuditLog(eq("task_updates"), eq(1L), eq("DELETE"), any(), isNull());
        verify(taskRepository, times(1)).save(any()); // Task actual_qty should be updated
    }

    @Test
    void testGetUpdatesForTask_WithLockedDates_ShouldMarkAsLocked() {
        // Arrange
        Wbs wbs = new Wbs();
        wbs.setWbsId(1L);
        task.setWbs(wbs);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(planVersionRepository.findCurrentByTaskId(1L)).thenReturn(Optional.empty());
        when(taskUpdateRepository.findByTaskIdOrderByUpdateDate(1L, true))
                .thenReturn(Collections.emptyList());

        // Create confirmation lock
        Confirmation confirmation = new Confirmation();
        confirmation.setConfirmationDate(LocalDate.of(2025, 11, 10));
        when(confirmationRepository.findByEntityTypeAndEntityId(eq("WBS"), eq(1L)))
                .thenReturn(Optional.of(confirmation));

        // Act
        List<TaskUpdateDayWiseDTO> result = taskUpdateService.getUpdatesForTask(1L);

        // Assert
        assertNotNull(result);
        // Dates before or on 2025-11-10 should be marked as locked
        for (TaskUpdateDayWiseDTO dayWise : result) {
            if (dayWise.getUpdateDate().compareTo(LocalDate.of(2025, 11, 10)) <= 0) {
                assertTrue(dayWise.getIsLocked(), "Date " + dayWise.getUpdateDate() + " should be locked");
            }
        }
    }
}

