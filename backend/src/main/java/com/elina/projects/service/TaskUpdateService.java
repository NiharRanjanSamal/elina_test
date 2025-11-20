package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.TaskUpdateCreateDTO;
import com.elina.projects.dto.TaskUpdateDTO;
import com.elina.projects.entity.Task;
import com.elina.projects.entity.TaskUpdate;
import com.elina.projects.repository.TaskRepository;
import com.elina.projects.repository.TaskUpdateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public TaskUpdateService(TaskUpdateRepository taskUpdateRepository,
                            TaskRepository taskRepository,
                            BusinessRuleEngine businessRuleEngine,
                            AuditLogService auditLogService) {
        this.taskUpdateRepository = taskUpdateRepository;
        this.taskRepository = taskRepository;
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
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Task not found");
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
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Task not found");
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
}

