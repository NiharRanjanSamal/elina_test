package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.*;
import com.elina.projects.entity.*;
import com.elina.projects.exception.NotFoundException;
import com.elina.projects.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for task update management with tenant-aware operations and business rule validation.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id from TenantContext.
 * Task updates are tenant-specific.
 * 
 * Business rules validated:
 * - Rule 101: BACKDATE_ALLOWED_TILL - Update date cannot be older than allowed backdate
 * - Rule 201: START_DATE_CANNOT_BE_IN_FUTURE - Update date cannot be in future
 * - Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY - Daily update qty cannot exceed planned_qty
 * - Rule 102: Update date cannot be after lock date if backdate not allowed
 */
@Service
public class TaskUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(TaskUpdateService.class);

    private final TaskUpdateRepository taskUpdateRepository;
    private final TaskRepository taskRepository;
    private final PlanLineRepository planLineRepository;
    private final PlanVersionRepository planVersionRepository;
    private final ConfirmationRepository confirmationRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public TaskUpdateService(TaskUpdateRepository taskUpdateRepository,
                            TaskRepository taskRepository,
                            PlanLineRepository planLineRepository,
                            PlanVersionRepository planVersionRepository,
                            ConfirmationRepository confirmationRepository,
                            BusinessRuleEngine businessRuleEngine,
                            AuditLogService auditLogService) {
        this.taskUpdateRepository = taskUpdateRepository;
        this.taskRepository = taskRepository;
        this.planLineRepository = planLineRepository;
        this.planVersionRepository = planVersionRepository;
        this.confirmationRepository = confirmationRepository;
        this.businessRuleEngine = businessRuleEngine;
        this.auditLogService = auditLogService;
    }

    /**
     * Get current user ID from SecurityContext.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Convert entity to DTO.
     */
    private TaskUpdateDTO toDTO(TaskUpdate entity) {
        TaskUpdateDTO dto = new TaskUpdateDTO();
        dto.setUpdateId(entity.getUpdateId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setTaskId(entity.getTask().getTaskId());
        dto.setUpdateDate(entity.getUpdateDate());
        dto.setPlannedQty(entity.getPlannedQty());
        dto.setActualQty(entity.getActualQty());
        dto.setDailyUpdateQty(entity.getDailyUpdateQty());
        dto.setRemarks(entity.getRemarks());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * List task updates for a task.
     */
    @Transactional(readOnly = true)
    public List<TaskUpdateDTO> getTaskUpdates(Long taskId) {
        Long tenantId = TenantContext.getTenantId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        List<TaskUpdate> updates = taskUpdateRepository.findByTaskId(taskId, true);
        return updates.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Create or update a day-wise task update.
     */
    @Transactional
    public TaskUpdateDTO createOrUpdateTaskUpdate(TaskUpdateCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Fetch task and existing data
        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Check if update already exists for this date
        TaskUpdate existingUpdate = taskUpdateRepository.findByTaskIdAndUpdateDate(dto.getTaskId(), dto.getUpdateDate())
                .orElse(null);

        // Calculate daily update qty if not provided
        BigDecimal dailyUpdateQty = dto.getDailyUpdateQty();
        if (dailyUpdateQty == null) {
            if (existingUpdate != null) {
                // Calculate difference from previous actual qty
                dailyUpdateQty = dto.getActualQty().subtract(existingUpdate.getActualQty());
            } else {
                // New update - use actual qty as daily update
                dailyUpdateQty = dto.getActualQty();
            }
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("TASK_UPDATE")
                .entityId(dto.getTaskId())
                .updateDate(dto.getUpdateDate())
                .plannedQty(dto.getPlannedQty() != null ? dto.getPlannedQty() : task.getPlannedQty())
                .actualQty(dto.getActualQty())
                .dailyUpdateQty(dailyUpdateQty)
                .taskStartDate(task.getStartDate())
                .lockDate(task.getLockDate())
                .isConfirmed(task.getIsConfirmed())
                .build();

        // Validate business rules BEFORE proceeding with update
        // Rules validated: 101 (BACKDATE_ALLOWED_TILL), 201 (START_DATE_CANNOT_BE_IN_FUTURE), 401 (DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY)
        try {
            businessRuleEngine.validateAll(
                Arrays.asList(101, 201, 401), // Rules to validate
                context
            );
        } catch (BusinessRuleException e) {
            // Log the violation
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            // Re-throw - GlobalExceptionHandler will handle it
            throw e;
        }

        // If we get here, all rules passed - proceed with update
        TaskUpdate entity = existingUpdate != null ? existingUpdate : new TaskUpdate();
        
        if (existingUpdate == null) {
            // New update
            entity.setTenant(task.getTenant());
            entity.setTask(task);
            entity.setCreatedBy(userId);
        }
        
        entity.setUpdateDate(dto.getUpdateDate());
        entity.setPlannedQty(dto.getPlannedQty() != null ? dto.getPlannedQty() : task.getPlannedQty());
        entity.setActualQty(dto.getActualQty());
        entity.setDailyUpdateQty(dailyUpdateQty);
        entity.setRemarks(dto.getRemarks());
        entity.setActivateFlag(true);
        entity.setUpdatedBy(userId);

        TaskUpdate saved = taskUpdateRepository.save(entity);
        
        // Update task's actual qty (aggregate from all updates)
        BigDecimal totalActualQty = taskUpdateRepository.getTotalActualQtyByTaskId(dto.getTaskId());
        task.setActualQty(totalActualQty);
        taskRepository.save(task);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("updateDate", saved.getUpdateDate());
        newData.put("actualQty", saved.getActualQty());
        newData.put("dailyUpdateQty", saved.getDailyUpdateQty());
        auditLogService.writeAuditLog("task_updates", saved.getUpdateId(), 
            existingUpdate != null ? "UPDATE" : "INSERT", 
            existingUpdate != null ? Map.of("actualQty", existingUpdate.getActualQty()) : null, 
            newData);
        
        logger.info("Created/updated day-wise update for task {} on date {} for tenant {}", 
            dto.getTaskId(), dto.getUpdateDate(), tenantId);
        return toDTO(saved);
    }

    /**
     * Get unified day-wise updates for a task.
     * Merges plan lines from active plan version with existing task updates.
     * For date gaps: plan_qty from plan_lines, actual_qty = 0.
     * 
     * @param taskId Task ID
     * @return List of unified day-wise updates
     */
    @Transactional(readOnly = true)
    public List<TaskUpdateDayWiseDTO> getUpdatesForTask(Long taskId) {
        Long tenantId = TenantContext.getTenantId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Get active plan version for this task
        PlanVersion activePlanVersion = planVersionRepository.findCurrentByTaskId(taskId)
                .orElse(null);

        // Get plan lines from active plan version (if exists)
        Map<LocalDate, BigDecimal> planQtyMap = new HashMap<>();
        if (activePlanVersion != null) {
            List<PlanLine> planLines = planLineRepository.findByPlanVersionId(
                activePlanVersion.getPlanVersionId(), true);
            for (PlanLine planLine : planLines) {
                planQtyMap.put(planLine.getWorkDate(), planLine.getPlannedQty());
            }
        }

        // Get existing task updates
        List<TaskUpdate> existingUpdates = taskUpdateRepository.findByTaskIdOrderByUpdateDate(taskId, true);
        Map<LocalDate, TaskUpdate> updateMap = existingUpdates.stream()
                .collect(Collectors.toMap(TaskUpdate::getUpdateDate, u -> u));

        // Get confirmation lock for WBS (if exists)
        Confirmation wbsConfirmation = task.getWbs() != null 
            ? confirmationRepository.findByEntityTypeAndEntityId("WBS", task.getWbs().getWbsId()).orElse(null)
            : null;
        LocalDate lockDate = wbsConfirmation != null ? wbsConfirmation.getConfirmationDate() : null;

        // Build unified list
        List<TaskUpdateDayWiseDTO> result = new ArrayList<>();
        
        // Get date range from task
        LocalDate startDate = task.getStartDate();
        LocalDate endDate = task.getEndDate();
        
        if (startDate != null && endDate != null) {
            // Generate entries for all dates in task range
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                TaskUpdate existingUpdate = updateMap.get(currentDate);
                BigDecimal planQty = planQtyMap.getOrDefault(currentDate, 
                    existingUpdate != null ? existingUpdate.getPlannedQty() : null);
                BigDecimal actualQty = existingUpdate != null ? existingUpdate.getActualQty() : BigDecimal.ZERO;
                
                TaskUpdateDayWiseDTO dto = new TaskUpdateDayWiseDTO(currentDate, planQty, actualQty);
                dto.setUpdateId(existingUpdate != null ? existingUpdate.getUpdateId() : null);
                dto.setRemarks(existingUpdate != null ? existingUpdate.getRemarks() : null);
                
                // Check if date is locked
                boolean isLocked = lockDate != null && !currentDate.isAfter(lockDate);
                dto.setIsLocked(isLocked);
                dto.setCanEdit(!isLocked); // Can edit if not locked
                
                result.add(dto);
                currentDate = currentDate.plusDays(1);
            }
        } else {
            // If no date range, just return existing updates
            for (TaskUpdate update : existingUpdates) {
                TaskUpdateDayWiseDTO dto = new TaskUpdateDayWiseDTO(
                    update.getUpdateDate(), 
                    update.getPlannedQty(), 
                    update.getActualQty());
                dto.setUpdateId(update.getUpdateId());
                dto.setRemarks(update.getRemarks());
                
                boolean isLocked = lockDate != null && !update.getUpdateDate().isAfter(lockDate);
                dto.setIsLocked(isLocked);
                dto.setCanEdit(!isLocked);
                
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * Save or update multiple day-wise updates in bulk.
     * Validates business rules for each entry.
     * 
     * @param bulkDTO Bulk update DTO containing task ID and list of day-wise updates
     * @return List of saved/updated task update DTOs
     */
    @Transactional
    public List<TaskUpdateDTO> saveOrUpdateDayWise(TaskUpdateBulkDTO bulkDTO) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Task task = taskRepository.findById(bulkDTO.getTaskId())
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Get confirmation lock for WBS
        Confirmation wbsConfirmation = task.getWbs() != null 
            ? confirmationRepository.findByEntityTypeAndEntityId("WBS", task.getWbs().getWbsId()).orElse(null)
            : null;
        LocalDate lockDate = wbsConfirmation != null ? wbsConfirmation.getConfirmationDate() : null;

        List<TaskUpdateDTO> results = new ArrayList<>();

        for (TaskUpdateBulkDTO.DayWiseUpdateDTO dayUpdate : bulkDTO.getUpdates()) {
            // Validate update date is within task range
            if (task.getStartDate() != null && dayUpdate.getUpdateDate().isBefore(task.getStartDate())) {
                throw new BusinessRuleException(null, 
                    "Update date " + dayUpdate.getUpdateDate() + " is before task start date " + task.getStartDate());
            }
            if (task.getEndDate() != null && dayUpdate.getUpdateDate().isAfter(task.getEndDate())) {
                throw new BusinessRuleException(null, 
                    "Update date " + dayUpdate.getUpdateDate() + " is after task end date " + task.getEndDate());
            }

            // Check if date is locked
            if (lockDate != null && !dayUpdate.getUpdateDate().isAfter(lockDate)) {
                // Check Rule 102: BACKDATE_ALLOWED_AFTER_LOCK
                BusinessRuleContext lockContext = BusinessRuleContext.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .entityType("TASK_UPDATE")
                        .entityId(bulkDTO.getTaskId())
                        .updateDate(dayUpdate.getUpdateDate())
                        .confirmationDate(lockDate)
                        .build();
                
                try {
                    businessRuleEngine.validate(102, lockContext);
                } catch (BusinessRuleException e) {
                    logger.warn("Update blocked for locked date {}: Rule {} - {}", 
                        dayUpdate.getUpdateDate(), e.getRuleNumber(), e.getMessage());
                    throw new BusinessRuleException(102, 
                        "Cannot update date " + dayUpdate.getUpdateDate() + " - date is locked by confirmation. " + e.getMessage());
                }
            }

            // Get existing update if any
            TaskUpdate existingUpdate = taskUpdateRepository.findByTaskIdAndUpdateDate(
                bulkDTO.getTaskId(), dayUpdate.getUpdateDate()).orElse(null);

            // Calculate daily update qty
            BigDecimal dailyUpdateQty = BigDecimal.ZERO;
            if (existingUpdate != null) {
                dailyUpdateQty = dayUpdate.getActualQty().subtract(existingUpdate.getActualQty());
            } else {
                dailyUpdateQty = dayUpdate.getActualQty();
            }

            // Build context for business rule validation
            BusinessRuleContext context = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("TASK_UPDATE")
                    .entityId(bulkDTO.getTaskId())
                    .updateDate(dayUpdate.getUpdateDate())
                    .plannedQty(dayUpdate.getPlanQty() != null ? dayUpdate.getPlanQty() : task.getPlannedQty())
                    .actualQty(dayUpdate.getActualQty())
                    .dailyUpdateQty(dailyUpdateQty)
                    .taskStartDate(task.getStartDate())
                    .taskEndDate(task.getEndDate())
                    .lockDate(lockDate)
                    .isConfirmed(task.getIsConfirmed())
                    .build();

            // Validate business rules
            try {
                businessRuleEngine.validateAll(Arrays.asList(101, 201, 401), context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation for date {}: Rule {} - {}", 
                    dayUpdate.getUpdateDate(), e.getRuleNumber(), e.getMessage());
                throw e;
            }

            // Save or update
            TaskUpdate entity = existingUpdate != null ? existingUpdate : new TaskUpdate();
            
            if (existingUpdate == null) {
                entity.setTenant(task.getTenant());
                entity.setTask(task);
                entity.setCreatedBy(userId);
            }
            
            entity.setUpdateDate(dayUpdate.getUpdateDate());
            entity.setPlannedQty(dayUpdate.getPlanQty() != null ? dayUpdate.getPlanQty() : task.getPlannedQty());
            entity.setActualQty(dayUpdate.getActualQty());
            entity.setDailyUpdateQty(dailyUpdateQty);
            entity.setRemarks(dayUpdate.getRemarks());
            entity.setActivateFlag(true);
            entity.setUpdatedBy(userId);

            TaskUpdate saved = taskUpdateRepository.save(entity);
            
            // Write audit log
            Map<String, Object> newData = new HashMap<>();
            newData.put("updateDate", saved.getUpdateDate());
            newData.put("actualQty", saved.getActualQty());
            newData.put("dailyUpdateQty", saved.getDailyUpdateQty());
            auditLogService.writeAuditLog("task_updates", saved.getUpdateId(), 
                existingUpdate != null ? "UPDATE" : "INSERT", 
                existingUpdate != null ? Map.of("actualQty", existingUpdate.getActualQty()) : null, 
                newData);
            
            results.add(toDTO(saved));
        }

        // Update task's actual qty (aggregate from all updates)
        BigDecimal totalActualQty = taskUpdateRepository.getTotalActualQtyByTaskId(bulkDTO.getTaskId());
        task.setActualQty(totalActualQty);
        taskRepository.save(task);

        logger.info("Bulk saved {} day-wise updates for task {} for tenant {}", 
            results.size(), bulkDTO.getTaskId(), tenantId);
        return results;
    }

    /**
     * Delete a task update.
     * Only allowed if no confirmation lock covers update_date and backdate rules permit.
     * 
     * @param updateId Update ID to delete
     */
    @Transactional
    public void deleteTaskUpdate(Long updateId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        TaskUpdate update = taskUpdateRepository.findById(updateId)
                .orElseThrow(() -> new NotFoundException("Task update not found"));

        if (!update.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task update not found");
        }

        Task task = update.getTask();

        // Check confirmation lock
        Confirmation wbsConfirmation = task.getWbs() != null 
            ? confirmationRepository.findByEntityTypeAndEntityId("WBS", task.getWbs().getWbsId()).orElse(null)
            : null;
        LocalDate lockDate = wbsConfirmation != null ? wbsConfirmation.getConfirmationDate() : null;

        if (lockDate != null && !update.getUpdateDate().isAfter(lockDate)) {
            // Check Rule 102: BACKDATE_ALLOWED_AFTER_LOCK
            BusinessRuleContext lockContext = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("TASK_UPDATE")
                    .entityId(task.getTaskId())
                    .updateDate(update.getUpdateDate())
                    .confirmationDate(lockDate)
                    .build();
            
            try {
                businessRuleEngine.validate(102, lockContext);
            } catch (BusinessRuleException e) {
                throw new BusinessRuleException(102, 
                    "Cannot delete update for locked date " + update.getUpdateDate() + ". " + e.getMessage());
            }
        }

        // Validate backdate rule (Rule 101)
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("TASK_UPDATE")
                .entityId(task.getTaskId())
                .updateDate(update.getUpdateDate())
                .build();

        try {
            businessRuleEngine.validate(101, context);
        } catch (BusinessRuleException e) {
            throw new BusinessRuleException(101, 
                "Cannot delete update for date " + update.getUpdateDate() + ". " + e.getMessage());
        }

        // Write audit log before deletion
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("updateDate", update.getUpdateDate());
        oldData.put("actualQty", update.getActualQty());
        auditLogService.writeAuditLog("task_updates", update.getUpdateId(), "DELETE", oldData, null);

        // Delete
        taskUpdateRepository.delete(update);

        // Update task's actual qty
        BigDecimal totalActualQty = taskUpdateRepository.getTotalActualQtyByTaskId(task.getTaskId());
        task.setActualQty(totalActualQty);
        taskRepository.save(task);

        logger.info("Deleted task update {} for task {} on date {} for tenant {}", 
            updateId, task.getTaskId(), update.getUpdateDate(), tenantId);
    }

    /**
     * Get daily summary (plan_qty, actual_qty, variance) for a task within date range.
     * Used for reporting.
     * 
     * @param taskId Task ID
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of daily summaries
     */
    @Transactional(readOnly = true)
    public List<TaskUpdateSummaryDTO> getDailySummary(Long taskId, LocalDate fromDate, LocalDate toDate) {
        Long tenantId = TenantContext.getTenantId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Get plan lines from active plan version
        PlanVersion activePlanVersion = planVersionRepository.findCurrentByTaskId(taskId).orElse(null);
        Map<LocalDate, BigDecimal> planQtyMap = new HashMap<>();
        if (activePlanVersion != null) {
            List<PlanLine> planLines = planLineRepository.findByPlanVersionId(
                activePlanVersion.getPlanVersionId(), true);
            for (PlanLine planLine : planLines) {
                if (planLine.getWorkDate().compareTo(fromDate) >= 0 && 
                    planLine.getWorkDate().compareTo(toDate) <= 0) {
                    planQtyMap.put(planLine.getWorkDate(), planLine.getPlannedQty());
                }
            }
        }

        // Get task updates in date range
        List<TaskUpdate> updates = taskUpdateRepository.findByTaskIdAndDateRange(
            taskId, fromDate, toDate, true);

        Map<LocalDate, TaskUpdate> updateMap = updates.stream()
                .collect(Collectors.toMap(TaskUpdate::getUpdateDate, u -> u));

        // Build summary list
        List<TaskUpdateSummaryDTO> result = new ArrayList<>();
        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            TaskUpdate update = updateMap.get(currentDate);
            BigDecimal planQty = planQtyMap.getOrDefault(currentDate, 
                update != null ? update.getPlannedQty() : null);
            BigDecimal actualQty = update != null ? update.getActualQty() : BigDecimal.ZERO;
            
            result.add(new TaskUpdateSummaryDTO(currentDate, planQty, actualQty));
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }
}

