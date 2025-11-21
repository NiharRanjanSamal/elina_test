package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.*;
import com.elina.projects.entity.Project;
import com.elina.projects.entity.Task;
import com.elina.projects.entity.Wbs;
import com.elina.projects.exception.NotFoundException;
import com.elina.projects.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for task management with tenant-aware operations and business rule validation.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id from TenantContext.
 * Tasks are tenant-specific.
 * 
 * Business rules validated:
 * - Rule 201: START_DATE_CANNOT_BE_IN_FUTURE - Task start_date cannot be in future
 * - Rule 202: Task end_date must be after start_date
 * - Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN - Confirmed tasks cannot be modified
 */
@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final TaskUpdateRepository taskUpdateRepository;
    private final PlanVersionRepository planVersionRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public TaskService(TaskRepository taskRepository,
                      WbsRepository wbsRepository,
                      ProjectRepository projectRepository,
                      TenantRepository tenantRepository,
                      TaskUpdateRepository taskUpdateRepository,
                      PlanVersionRepository planVersionRepository,
                      BusinessRuleEngine businessRuleEngine,
                      AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.wbsRepository = wbsRepository;
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.taskUpdateRepository = taskUpdateRepository;
        this.planVersionRepository = planVersionRepository;
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
    private TaskDTO toDTO(Task entity) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setProjectId(entity.getProject().getProjectId());
        dto.setWbsId(entity.getWbs().getWbsId());
        dto.setTaskCode(entity.getTaskCode());
        dto.setTaskName(entity.getTaskName());
        dto.setDescription(entity.getDescription());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setPlannedQty(entity.getPlannedQty());
        dto.setActualQty(entity.getActualQty());
        dto.setUnit(entity.getUnit());
        dto.setStatus(entity.getStatus());
        dto.setIsConfirmed(entity.getIsConfirmed());
        dto.setConfirmedOn(entity.getConfirmedOn());
        dto.setConfirmedBy(entity.getConfirmedBy());
        dto.setIsLocked(entity.getIsLocked());
        dto.setLockDate(entity.getLockDate());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * List tasks with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<TaskDTO> listTasks(Long projectId, Long wbsId, Boolean activeOnly, String search, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Task> entities = taskRepository.findWithFilters(projectId, wbsId, activeOnly, search, pageable);
        return entities.map(this::toDTO);
    }

    /**
     * Get task by ID.
     */
    @Transactional(readOnly = true)
    public TaskDTO getTask(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Task entity = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        
        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }
        
        return toDTO(entity);
    }

    /**
     * Get task details with plan versions and updates.
     */
    @Transactional(readOnly = true)
    public TaskDetailsDTO getTaskDetails(Long taskId) {
        TaskDTO task = getTask(taskId);
        
        // Get plan versions
        List<PlanVersionDTO> planVersions = planVersionRepository.findByTaskId(taskId, true)
                .stream()
                .map(this::toPlanVersionDTO)
                .collect(Collectors.toList());
        
        // Get current plan version
        PlanVersionDTO currentPlanVersion = planVersionRepository.findCurrentByTaskId(taskId)
                .map(this::toPlanVersionDTO)
                .orElse(null);
        
        // Get recent updates
        List<TaskUpdateDTO> recentUpdates = taskUpdateRepository.findByTaskId(taskId, true)
                .stream()
                .limit(10)
                .map(this::toTaskUpdateDTO)
                .collect(Collectors.toList());
        
        TaskDetailsDTO details = new TaskDetailsDTO();
        details.setTask(task);
        details.setPlanVersions(planVersions);
        details.setCurrentPlanVersion(currentPlanVersion);
        details.setRecentUpdates(recentUpdates);
        details.setUpdateCount(recentUpdates.size());
        
        return details;
    }

    /**
     * Convert PlanVersion entity to DTO.
     */
    private PlanVersionDTO toPlanVersionDTO(com.elina.projects.entity.PlanVersion entity) {
        PlanVersionDTO dto = new PlanVersionDTO();
        dto.setPlanVersionId(entity.getPlanVersionId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setTaskId(entity.getTask().getTaskId());
        dto.setVersionNo(entity.getVersionNo());
        dto.setVersionDate(entity.getVersionDate());
        dto.setDescription(entity.getDescription());
        dto.setIsActive(entity.getIsActive());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * Convert TaskUpdate entity to DTO.
     */
    private TaskUpdateDTO toTaskUpdateDTO(com.elina.projects.entity.TaskUpdate entity) {
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
     * Get all tasks for a WBS.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByWbsId(Long wbsId) {
        Long tenantId = TenantContext.getTenantId();

        Wbs wbs = wbsRepository.findById(wbsId)
                .orElseThrow(() -> new NotFoundException("WBS not found"));
        
        if (!wbs.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("WBS not found");
        }

        List<Task> tasks = taskRepository.findByWbsId(wbsId, true);
        return tasks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Create a new task.
     */
    @Transactional
    public TaskDTO createTask(TaskCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Verify project and WBS belong to tenant
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }

        Wbs wbs = wbsRepository.findById(dto.getWbsId())
                .orElseThrow(() -> new NotFoundException("WBS not found"));
        
        if (!wbs.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("WBS not found");
        }

        // Verify WBS belongs to project
        if (!wbs.getProject().getProjectId().equals(dto.getProjectId())) {
            throw new RuntimeException("WBS does not belong to project");
        }

        // Check if task code already exists
        if (taskRepository.existsByTaskCode(dto.getTaskCode())) {
            throw new RuntimeException("Task with code " + dto.getTaskCode() + " already exists");
        }

        // Validate task dates against parent WBS dates
        if (dto.getStartDate() != null && wbs.getStartDate() != null) {
            if (dto.getStartDate().isBefore(wbs.getStartDate())) {
                throw new BusinessRuleException(202,
                    "Task start date (" + dto.getStartDate() + ") cannot be before WBS start date (" + wbs.getStartDate() + ").",
                    "Task start date must be on or after WBS start date.");
            }
        }

        if (dto.getEndDate() != null && wbs.getEndDate() != null) {
            if (dto.getEndDate().isAfter(wbs.getEndDate())) {
                throw new BusinessRuleException(202,
                    "Task end date (" + dto.getEndDate() + ") cannot be after WBS end date (" + wbs.getEndDate() + ").",
                    "Task end date must be on or before WBS end date.");
            }
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("TASK")
                .taskStartDate(dto.getStartDate())
                .taskEndDate(dto.getEndDate())
                .wbsStartDate(wbs.getStartDate())
                .wbsEndDate(wbs.getEndDate())
                .build();

        // Validate Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
        if (dto.getStartDate() != null) {
            try {
                businessRuleEngine.validate(201, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        // Validate Rule 202: Task date range
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BusinessRuleException(202,
                    "Task end date (" + dto.getEndDate() + ") cannot be before start date (" + dto.getStartDate() + ").",
                    "Task end date must be on or after the start date.");
            }
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Task entity = new Task();
        entity.setTenant(tenant);
        entity.setProject(project);
        entity.setWbs(wbs);
        entity.setTaskCode(dto.getTaskCode());
        entity.setTaskName(dto.getTaskName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setPlannedQty(dto.getPlannedQty());
        entity.setUnit(dto.getUnit());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "PLANNED");
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        Task saved = taskRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("taskCode", saved.getTaskCode());
        newData.put("taskName", saved.getTaskName());
        newData.put("startDate", saved.getStartDate());
        newData.put("endDate", saved.getEndDate());
        auditLogService.writeAuditLog("tasks", saved.getTaskId(), "INSERT", null, newData);
        
        logger.info("Created task: {} for tenant {}", dto.getTaskCode(), tenantId);
        return toDTO(saved);
    }

    /**
     * Update an existing task.
     */
    @Transactional
    public TaskDTO updateTask(Long id, TaskCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Task entity = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Validate Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
        if (entity.getIsConfirmed() != null && entity.getIsConfirmed()) {
            BusinessRuleContext context = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("TASK")
                    .entityId(id)
                    .isConfirmed(true)
                    .isLocked(entity.getIsLocked())
                    .build();
            
            try {
                businessRuleEngine.validate(301, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        // Check if task code change would create duplicate
        if (!entity.getTaskCode().equals(dto.getTaskCode()) && 
            taskRepository.existsByTaskCode(dto.getTaskCode())) {
            throw new RuntimeException("Task with code " + dto.getTaskCode() + " already exists");
        }

        Wbs wbs = entity.getWbs();
        
        // Validate task dates against parent WBS dates
        if (dto.getStartDate() != null && wbs.getStartDate() != null) {
            if (dto.getStartDate().isBefore(wbs.getStartDate())) {
                throw new BusinessRuleException(202,
                    "Task start date (" + dto.getStartDate() + ") cannot be before WBS start date (" + wbs.getStartDate() + ").",
                    "Task start date must be on or after WBS start date.");
            }
        }

        if (dto.getEndDate() != null && wbs.getEndDate() != null) {
            if (dto.getEndDate().isAfter(wbs.getEndDate())) {
                throw new BusinessRuleException(202,
                    "Task end date (" + dto.getEndDate() + ") cannot be after WBS end date (" + wbs.getEndDate() + ").",
                    "Task end date must be on or before WBS end date.");
            }
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("TASK")
                .entityId(id)
                .taskStartDate(dto.getStartDate())
                .taskEndDate(dto.getEndDate())
                .wbsStartDate(wbs.getStartDate())
                .wbsEndDate(wbs.getEndDate())
                .build();

        // Validate Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
        if (dto.getStartDate() != null) {
            try {
                businessRuleEngine.validate(201, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        // Validate Rule 202: Task date range
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BusinessRuleException(202,
                    "Task end date (" + dto.getEndDate() + ") cannot be before start date (" + dto.getStartDate() + ").",
                    "Task end date must be on or after the start date.");
            }
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("taskCode", entity.getTaskCode());
        oldData.put("taskName", entity.getTaskName());
        oldData.put("startDate", entity.getStartDate());
        oldData.put("endDate", entity.getEndDate());
        oldData.put("plannedQty", entity.getPlannedQty());
        oldData.put("status", entity.getStatus());

        entity.setTaskCode(dto.getTaskCode());
        entity.setTaskName(dto.getTaskName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setPlannedQty(dto.getPlannedQty());
        entity.setUnit(dto.getUnit());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : entity.getStatus());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : entity.getActivateFlag());
        entity.setUpdatedBy(userId);

        Task saved = taskRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("taskCode", saved.getTaskCode());
        newData.put("taskName", saved.getTaskName());
        newData.put("startDate", saved.getStartDate());
        newData.put("endDate", saved.getEndDate());
        newData.put("plannedQty", saved.getPlannedQty());
        newData.put("status", saved.getStatus());
        auditLogService.writeAuditLog("tasks", saved.getTaskId(), "UPDATE", oldData, newData);
        
        logger.info("Updated task: {} for tenant {}", dto.getTaskCode(), tenantId);
        return toDTO(saved);
    }

    /**
     * Delete a task (soft delete - set activateFlag to false).
     */
    @Transactional
    public void deleteTask(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Task entity = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("taskCode", entity.getTaskCode());
        oldData.put("activateFlag", entity.getActivateFlag());

        entity.setActivateFlag(false);
        taskRepository.save(entity);
        
        // Write audit log
        auditLogService.writeAuditLog("tasks", id, "DELETE", oldData, null);
        
        logger.info("Deleted task: {} for tenant {}", id, tenantId);
    }
}

