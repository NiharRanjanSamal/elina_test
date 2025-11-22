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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for plan version management with tenant-aware operations and business rule validation.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id from TenantContext.
 * Plan versions are tenant-specific.
 * 
 * Business rules validated:
 * - Rule 402: PLAN_VERSION_DATE_VALIDATION - Plan version date cannot be in future
 */
@Service
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
    private static final int RULE_BACKDATE_AFTER_LOCK = 102;

    private final PlanVersionRepository planVersionRepository;
    private final PlanLineRepository planLineRepository;
    private final TaskRepository taskRepository;
    private final TenantRepository tenantRepository;
    private final ConfirmationLockRepository confirmationLockRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public PlanService(PlanVersionRepository planVersionRepository,
            PlanLineRepository planLineRepository,
            TaskRepository taskRepository,
            TenantRepository tenantRepository,
            ConfirmationLockRepository confirmationLockRepository,
            BusinessRuleEngine businessRuleEngine,
            AuditLogService auditLogService) {
        this.planVersionRepository = planVersionRepository;
        this.planLineRepository = planLineRepository;
        this.taskRepository = taskRepository;
        this.tenantRepository = tenantRepository;
        this.confirmationLockRepository = confirmationLockRepository;
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
     * Convert PlanVersion entity to DTO.
     */
    private PlanVersionDTO toDTO(PlanVersion entity) {
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
     * Convert PlanLine entity to DTO.
     */
    private PlanLineDTO toLineDTO(PlanLine entity) {
        PlanLineDTO dto = new PlanLineDTO();
        dto.setPlanLineId(entity.getPlanLineId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setPlanVersionId(entity.getPlanVersion().getPlanVersionId());
        dto.setTaskId(entity.getTask().getTaskId());
        dto.setLineNumber(entity.getLineNumber());
        dto.setWorkDate(entity.getWorkDate());
        dto.setPlannedQty(entity.getPlannedQty());
        dto.setDescription(entity.getDescription());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * List plan versions for a task.
     */
    @Transactional(readOnly = true)
    public List<PlanVersionDTO> listPlanVersions(Long taskId) {
        Long tenantId = TenantContext.getTenantId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        List<PlanVersion> versions = planVersionRepository.findByTaskId(taskId, true);
        return versions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get plan version with lines.
     */
    @Transactional(readOnly = true)
    public PlanVersionDTO getPlanVersion(Long planVersionId) {
        Long tenantId = TenantContext.getTenantId();

        PlanVersion version = planVersionRepository.findById(planVersionId)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));

        if (!version.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        return toDTO(version);
    }

    /**
     * Get plan lines for a version.
     */
    @Transactional(readOnly = true)
    public List<PlanLineDTO> getPlanLines(Long planVersionId) {
        Long tenantId = TenantContext.getTenantId();

        PlanVersion version = planVersionRepository.findById(planVersionId)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));

        if (!version.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        List<PlanLine> lines = planLineRepository.findByPlanVersionId(planVersionId, true);
        return lines.stream().map(this::toLineDTO).collect(Collectors.toList());
    }

    /**
     * Create a new plan version with lines.
     */
    @Transactional
    public PlanVersionDTO createPlanVersion(PlanVersionCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Fetch task
        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Validate plan lines dates are within task date range
        if (task.getStartDate() != null && task.getEndDate() != null) {
            for (PlanLineCreateDTO lineDto : dto.getLines()) {
                LocalDate workDate = lineDto.getWorkDate() != null ? lineDto.getWorkDate() : lineDto.getPlannedDate();
                if (workDate != null
                        && (workDate.isBefore(task.getStartDate()) || workDate.isAfter(task.getEndDate()))) {
                    throw new BusinessRuleException(202,
                            "Plan line date must be within task date range",
                            "Task date range: " + task.getStartDate() + " to " + task.getEndDate());
                }
            }
        }

        // Validate plan lines for business rules (101, 201, 202)
        LocalDate today = LocalDate.now();
        for (PlanLineCreateDTO lineDto : dto.getLines()) {
            LocalDate workDate = lineDto.getWorkDate() != null ? lineDto.getWorkDate() : lineDto.getPlannedDate();
            if (workDate == null)
                continue;

            // Rule 101: BACKDATE_ALLOWED_TILL
            BusinessRuleContext backdateContext = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("PLAN_LINE")
                    .entityId(dto.getTaskId())
                    .updateDate(workDate)
                    .build();
            try {
                businessRuleEngine.validate(101, backdateContext);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }

            // Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
            if (workDate.isAfter(today)) {
                BusinessRuleContext futureContext = BusinessRuleContext.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .entityType("PLAN_LINE")
                        .entityId(dto.getTaskId())
                        .updateDate(workDate)
                        .build();
                try {
                    businessRuleEngine.validate(201, futureContext);
                } catch (BusinessRuleException e) {
                    logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                    throw e;
                }
            }

            // Rule 202: END_DATE_CANNOT_BE_BEFORE_START_DATE (validated above with task range)
            if (lineDto.getPlannedQty() != null && lineDto.getPlannedQty().signum() < 0) {
                throw new BusinessRuleException(202,
                        "Planned quantity cannot be negative",
                        "Please enter a valid positive quantity");
            }
        }

        // Build BusinessRuleContext for version date validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("PLAN_VERSION")
                .entityId(dto.getTaskId())
                .planVersionDate(dto.getVersionDate())
                .build();

        // Validate Rule 402: PLAN_VERSION_DATE_VALIDATION
        try {
            businessRuleEngine.validate(402, context);
        } catch (BusinessRuleException e) {
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            throw e;
        }

        // Get next version number
        Integer maxVersion = planVersionRepository.findMaxVersionNoByTaskId(dto.getTaskId());
        Integer nextVersionNo = maxVersion != null ? maxVersion + 1 : 1;

        // Mark all previous versions as not active
        List<PlanVersion> previousVersions = planVersionRepository.findByTaskId(dto.getTaskId(), true);
        for (PlanVersion prevVersion : previousVersions) {
            prevVersion.setIsActive(false);
            planVersionRepository.save(prevVersion);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Create plan version
        PlanVersion version = new PlanVersion();
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNo(nextVersionNo);
        version.setVersionDate(dto.getVersionDate());
        version.setDescription(dto.getDescription());
        version.setIsActive(true);
        version.setActivateFlag(true);
        version.setCreatedBy(userId);
        version.setUpdatedBy(userId);

        PlanVersion saved = planVersionRepository.save(version);

        // Create plan lines
        for (PlanLineCreateDTO lineDto : dto.getLines()) {
            PlanLine line = new PlanLine();
            line.setTenant(tenant);
            line.setPlanVersion(saved);
            line.setTask(task);
            line.setLineNumber(lineDto.getLineNumber());
            LocalDate workDate = lineDto.getWorkDate() != null ? lineDto.getWorkDate() : lineDto.getPlannedDate();
            line.setWorkDate(workDate);
            line.setPlannedQty(lineDto.getPlannedQty());
            line.setDescription(lineDto.getDescription());
            line.setActivateFlag(true);
            line.setCreatedBy(userId);
            line.setUpdatedBy(userId);
            planLineRepository.save(line);
        }

        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("versionNo", saved.getVersionNo());
        newData.put("versionDate", saved.getVersionDate());
        newData.put("lineCount", dto.getLines().size());
        auditLogService.writeAuditLog("PLAN_VERSION", saved.getPlanVersionId(), "INSERT", null, newData);

        logger.info("Created plan version: {} for task {} for tenant {}",
                saved.getVersionNo(), dto.getTaskId(), tenantId);
        return toDTO(saved);
    }

    /**
     * Revert to a plan version (validate business rules on revert).
     */
    @Transactional
    public PlanVersionDTO revertToPlanVersion(Long planVersionId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        PlanVersion version = planVersionRepository.findById(planVersionId)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));

        if (!version.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("PLAN_VERSION")
                .entityId(version.getTask().getTaskId())
                .planVersionDate(version.getVersionDate())
                .build();

        // Validate Rule 402: PLAN_VERSION_DATE_VALIDATION
        try {
            businessRuleEngine.validate(402, context);
        } catch (BusinessRuleException e) {
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            throw e;
        }

        // Mark all previous versions as not active
        List<PlanVersion> previousVersions = planVersionRepository.findByTaskId(version.getTask().getTaskId(), true);
        for (PlanVersion prevVersion : previousVersions) {
            prevVersion.setIsActive(false);
            planVersionRepository.save(prevVersion);
        }

        // Set this version as active
        version.setIsActive(true);
        PlanVersion saved = planVersionRepository.save(version);

        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("versionNo", saved.getVersionNo());
        newData.put("isActive", true);
        auditLogService.writeAuditLog("PLAN_VERSION", saved.getPlanVersionId(), "UPDATE",
                Map.of("isActive", false), newData);

        logger.info("Reverted to plan version: {} for task {} for tenant {}",
                saved.getVersionNo(), saved.getTask().getTaskId(), tenantId);
        return toDTO(saved);
    }

    /**
     * Get plan version details with all lines (sorted by planned date).
     */
    @Transactional(readOnly = true)
    public PlanVersionDTO getPlanVersionDetails(Long planVersionId) {
        // Lines are fetched separately via getPlanLines endpoint
        return getPlanVersion(planVersionId);
    }

    /**
     * Set a plan version as active (without reverting).
     */
    @Transactional
    public PlanVersionDTO setActiveVersion(Long planVersionId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        PlanVersion version = planVersionRepository.findById(planVersionId)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));

        if (!version.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        // Mark all previous versions as not active
        List<PlanVersion> previousVersions = planVersionRepository.findByTaskId(version.getTask().getTaskId(), true);
        for (PlanVersion prevVersion : previousVersions) {
            if (prevVersion.getPlanVersionId().equals(planVersionId)) {
                continue; // Skip current version
            }
            prevVersion.setIsActive(false);
            planVersionRepository.save(prevVersion);
        }

        // Set this version as active
        version.setIsActive(true);
        version.setUpdatedBy(userId);
        PlanVersion saved = planVersionRepository.save(version);

        // Write audit log
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("isActive", false);
        Map<String, Object> newData = new HashMap<>();
        newData.put("versionNo", saved.getVersionNo());
        newData.put("isActive", true);
        auditLogService.writeAuditLog("PLAN_VERSION", saved.getPlanVersionId(), "UPDATE", oldData, newData);

        logger.info("Set plan version {} as active for task {} for tenant {}",
                saved.getVersionNo(), saved.getTask().getTaskId(), tenantId);
        return toDTO(saved);
    }

    /**
     * Create or update plan lines for a version.
     */
    @Transactional
    public PlanVersionDTO createOrUpdatePlanLines(Long planVersionId, List<PlanLineCreateDTO> lines, Long updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = updatedBy != null ? updatedBy : getCurrentUserId();

        PlanVersion version = planVersionRepository.findById(planVersionId)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));

        if (!version.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        Task task = version.getTask();

        // Validate plan lines dates are within task date range
        if (task.getStartDate() != null && task.getEndDate() != null) {
            for (PlanLineCreateDTO lineDto : lines) {
                LocalDate workDate = lineDto.getWorkDate() != null ? lineDto.getWorkDate() : lineDto.getPlannedDate();
                if (workDate != null
                        && (workDate.isBefore(task.getStartDate()) || workDate.isAfter(task.getEndDate()))) {
                    throw new BusinessRuleException(202,
                            "Plan line date must be within task date range",
                            "Task date range: " + task.getStartDate() + " to " + task.getEndDate());
                }
            }
        }

        // Validate business rules for each line
        LocalDate today = LocalDate.now();
        for (PlanLineCreateDTO lineDto : lines) {
            LocalDate workDate = lineDto.getWorkDate() != null ? lineDto.getWorkDate() : lineDto.getPlannedDate();
            if (workDate == null)
                continue;

            // Rule 101: BACKDATE_ALLOWED_TILL
            BusinessRuleContext backdateContext = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("PLAN_LINE")
                    .entityId(task.getTaskId())
                    .updateDate(workDate)
                    .build();
            try {
                businessRuleEngine.validate(101, backdateContext);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }

            // Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
            if (workDate.isAfter(today)) {
                BusinessRuleContext futureContext = BusinessRuleContext.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .entityType("PLAN_LINE")
                        .entityId(task.getTaskId())
                        .updateDate(workDate)
                        .build();
                try {
                    businessRuleEngine.validate(201, futureContext);
                } catch (BusinessRuleException e) {
                    logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                    throw e;
                }
            }

            // Rule 202: Quantity validation
            if (lineDto.getPlannedQty() != null && lineDto.getPlannedQty().signum() < 0) {
                throw new BusinessRuleException(202,
                        "Planned quantity cannot be negative",
                        "Please enter a valid positive quantity");
            }
        }

        // Get existing lines
        List<PlanLine> existingLines = planLineRepository.findByPlanVersionId(planVersionId, null);
        Map<LocalDate, PlanLine> existingByDate = existingLines.stream()
                .collect(Collectors.toMap(PlanLine::getWorkDate, line -> line, (l1, l2) -> l1));

        // Update or create lines
        int lineNumber = 1;
        for (PlanLineCreateDTO lineDto : lines) {
            LocalDate workDate = lineDto.getWorkDate() != null ? lineDto.getWorkDate() : lineDto.getPlannedDate();
            if (workDate == null)
                continue;

            PlanLine line = existingByDate.get(workDate);
            if (line != null) {
                // Update existing line
                line.setPlannedQty(lineDto.getPlannedQty());
                if (lineDto.getDescription() != null) {
                    line.setDescription(lineDto.getDescription());
                }
                line.setUpdatedBy(userId);
                planLineRepository.save(line);
            } else {
                // Create new line
                line = new PlanLine();
                line.setTenant(version.getTenant());
                line.setPlanVersion(version);
                line.setTask(task);
                line.setLineNumber(lineNumber++);
                line.setWorkDate(workDate);
                line.setPlannedQty(lineDto.getPlannedQty());
                line.setDescription(lineDto.getDescription());
                line.setActivateFlag(true);
                line.setCreatedBy(userId);
                line.setUpdatedBy(userId);
                planLineRepository.save(line);
            }
        }

        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("lineCount", lines.size());
        newData.put("versionNo", version.getVersionNo());
        auditLogService.writeAuditLog("PLAN_LINES", planVersionId, "UPDATE", null, newData);

        logger.info("Updated plan lines for version {} for task {} for tenant {}",
                version.getVersionNo(), task.getTaskId(), tenantId);
        return toDTO(version);
    }

    /**
     * Delete a plan version (allowed only if no confirmations applied against task).
     */
    @Transactional
    public void deletePlanVersion(Long planVersionId) {
        Long tenantId = TenantContext.getTenantId();

        PlanVersion version = planVersionRepository.findById(planVersionId)
                .orElseThrow(() -> new NotFoundException("Plan version not found"));

        if (!version.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        Task task = version.getTask();

        LocalDate lockDate = confirmationLockRepository.findLockDateByWbsId(task.getWbs().getWbsId());
        if (lockDate != null && !version.getVersionDate().isAfter(lockDate)) {
            throw new BusinessRuleException(RULE_BACKDATE_AFTER_LOCK,
                    String.format("Cannot delete plan version dated %s because WBS is locked till %s.",
                            version.getVersionDate(), lockDate),
                    "Undo or backdate the confirmation before deleting this plan version.");
        }

        // Delete plan lines first
        List<PlanLine> lines = planLineRepository.findByPlanVersionId(planVersionId, null);
        for (PlanLine line : lines) {
            planLineRepository.delete(line);
        }

        // Write audit log before deletion
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("versionNo", version.getVersionNo());
        oldData.put("taskId", task.getTaskId());
        oldData.put("lineCount", lines.size());
        auditLogService.writeAuditLog("PLAN_VERSION", planVersionId, "DELETE", oldData, null);

        // Delete plan version
        planVersionRepository.delete(version);

        logger.info("Deleted plan version {} for task {} for tenant {}",
                version.getVersionNo(), task.getTaskId(), tenantId);
    }

    /**
     * Create plan version using one of three modes: DAILY_ENTRY, DATE_RANGE_SPLIT, or SINGLE_LINE_QUICK.
     */
    @Transactional
    public PlanVersionDTO createPlanVersionWithMode(com.elina.projects.dto.PlanCreationModeDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Fetch task
        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (!task.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Task not found");
        }

        // Validate task date range exists
        if (task.getStartDate() == null || task.getEndDate() == null) {
            throw new BusinessRuleException(202,
                    "Task must have start and end dates before creating plan",
                    "Please set task dates first");
        }

        List<PlanLineCreateDTO> planLines = new java.util.ArrayList<>();

        // Generate plan lines based on mode
        switch (dto.getMode()) {
            case DAILY_ENTRY:
                if (dto.getDailyLines() == null || dto.getDailyLines().isEmpty()) {
                    throw new BusinessRuleException(202,
                            "Daily lines are required for DAILY_ENTRY mode",
                            "Please provide at least one daily plan line");
                }
                for (com.elina.projects.dto.PlanCreationModeDTO.DailyPlanLineDTO dailyLine : dto.getDailyLines()) {
                    PlanLineCreateDTO lineDto = new PlanLineCreateDTO();
                    lineDto.setWorkDate(dailyLine.getPlannedDate());
                    lineDto.setPlannedQty(dailyLine.getPlannedQty());
                    lineDto.setDescription(dailyLine.getDescription());
                    planLines.add(lineDto);
                }
                break;

            case DATE_RANGE_SPLIT:
                if (dto.getRangeSplit() == null) {
                    throw new BusinessRuleException(202,
                            "Range split configuration is required for DATE_RANGE_SPLIT mode",
                            "Please provide range split details");
                }
                planLines = generatePlanLinesFromRangeSplit(dto.getRangeSplit(), task);
                break;

            case SINGLE_LINE_QUICK:
                if (dto.getSingleLine() == null) {
                    throw new BusinessRuleException(202,
                            "Single line configuration is required for SINGLE_LINE_QUICK mode",
                            "Please provide single line details");
                }
                PlanLineCreateDTO lineDto = new PlanLineCreateDTO();
                lineDto.setWorkDate(dto.getSingleLine().getPlannedDate());
                lineDto.setPlannedQty(dto.getSingleLine().getPlannedQty());
                lineDto.setDescription(dto.getSingleLine().getDescription());
                planLines.add(lineDto);
                break;

            default:
                throw new IllegalArgumentException("Unknown creation mode: " + dto.getMode());
        }

        // Assign line numbers
        for (int i = 0; i < planLines.size(); i++) {
            planLines.get(i).setLineNumber(i + 1);
        }

        // Create plan version using standard method
        PlanVersionCreateDTO createDto = new PlanVersionCreateDTO();
        createDto.setTaskId(dto.getTaskId());
        createDto.setVersionDate(dto.getVersionDate());
        createDto.setDescription(dto.getDescription());
        createDto.setLines(planLines);

        return createPlanVersion(createDto);
    }

    /**
     * Generate plan lines from date range split configuration.
     */
    private List<PlanLineCreateDTO> generatePlanLinesFromRangeSplit(
            com.elina.projects.dto.PlanCreationModeDTO.DateRangeSplitDTO rangeSplit, Task task) {

        List<PlanLineCreateDTO> lines = new java.util.ArrayList<>();
        LocalDate startDate = rangeSplit.getStartDate();
        LocalDate endDate = rangeSplit.getEndDate();
        java.math.BigDecimal totalQty = rangeSplit.getTotalQty();

        // Validate range is within task dates
        if (startDate.isBefore(task.getStartDate()) || endDate.isAfter(task.getEndDate())) {
            throw new BusinessRuleException(202,
                    "Range dates must be within task date range",
                    "Task range: " + task.getStartDate() + " to " + task.getEndDate());
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException(202,
                    "Start date must be before end date",
                    "Please correct the date range");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

        switch (rangeSplit.getSplitType()) {
            case EQUAL_SPLIT:
                // Split equally across all days
                java.math.BigDecimal qtyPerDay = totalQty.divide(
                        java.math.BigDecimal.valueOf(daysBetween), 2, java.math.RoundingMode.HALF_UP);
                LocalDate currentDate = startDate;
                while (!currentDate.isAfter(endDate)) {
                    PlanLineCreateDTO line = new PlanLineCreateDTO();
                    line.setWorkDate(currentDate);
                    line.setPlannedQty(qtyPerDay);
                    lines.add(line);
                    currentDate = currentDate.plusDays(1);
                }
                break;

            case WEEKLY_SPLIT:
                // Split by weeks
                long weeks = (daysBetween + 6) / 7; // Round up
                java.math.BigDecimal qtyPerWeek = totalQty.divide(
                        java.math.BigDecimal.valueOf(weeks), 2, java.math.RoundingMode.HALF_UP);
                LocalDate weekStart = startDate;
                while (!weekStart.isAfter(endDate)) {
                    LocalDate weekEnd = weekStart.plusDays(6);
                    if (weekEnd.isAfter(endDate)) {
                        weekEnd = endDate;
                    }
                    long daysInWeek = java.time.temporal.ChronoUnit.DAYS.between(weekStart, weekEnd) + 1;
                    java.math.BigDecimal dailyQty = qtyPerWeek.divide(
                            java.math.BigDecimal.valueOf(daysInWeek), 2, java.math.RoundingMode.HALF_UP);

                    LocalDate date = weekStart;
                    while (!date.isAfter(weekEnd)) {
                        PlanLineCreateDTO line = new PlanLineCreateDTO();
                        line.setWorkDate(date);
                        line.setPlannedQty(dailyQty);
                        lines.add(line);
                        date = date.plusDays(1);
                    }
                    weekStart = weekStart.plusWeeks(1);
                }
                break;

            case MONTHLY_SPLIT:
                // Split by months
                LocalDate monthStart = startDate;
                int monthCount = 0;
                while (!monthStart.isAfter(endDate)) {
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    if (monthEnd.isAfter(endDate)) {
                        monthEnd = endDate;
                    }
                    monthCount++;
                    monthStart = monthStart.plusMonths(1).withDayOfMonth(1);
                }
                java.math.BigDecimal qtyPerMonth = totalQty.divide(
                        java.math.BigDecimal.valueOf(monthCount), 2, java.math.RoundingMode.HALF_UP);

                monthStart = startDate;
                while (!monthStart.isAfter(endDate)) {
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    if (monthEnd.isAfter(endDate)) {
                        monthEnd = endDate;
                    }
                    long daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;
                    java.math.BigDecimal dailyQty = qtyPerMonth.divide(
                            java.math.BigDecimal.valueOf(daysInMonth), 2, java.math.RoundingMode.HALF_UP);

                    LocalDate date = monthStart;
                    while (!date.isAfter(monthEnd)) {
                        PlanLineCreateDTO line = new PlanLineCreateDTO();
                        line.setWorkDate(date);
                        line.setPlannedQty(dailyQty);
                        lines.add(line);
                        date = date.plusDays(1);
                    }
                    monthStart = monthStart.plusMonths(1).withDayOfMonth(1);
                }
                break;

            case CUSTOM_SPLIT:
                // Custom split - use provided quantities
                if (rangeSplit.getCustomQuantities() == null ||
                        rangeSplit.getCustomQuantities().size() != rangeSplit.getSplitCount()) {
                    throw new BusinessRuleException(202,
                            "Custom quantities count must match split count",
                            "Please provide " + rangeSplit.getSplitCount() + " quantities");
                }
                long daysPerSplit = daysBetween / rangeSplit.getSplitCount();
                LocalDate splitStart = startDate;
                int splitIndex = 0;
                for (java.math.BigDecimal splitQty : rangeSplit.getCustomQuantities()) {
                    LocalDate splitEnd = splitIndex == rangeSplit.getSplitCount() - 1
                            ? endDate
                            : splitStart.plusDays(daysPerSplit - 1);
                    long daysInSplit = java.time.temporal.ChronoUnit.DAYS.between(splitStart, splitEnd) + 1;
                    java.math.BigDecimal dailyQty = splitQty.divide(
                            java.math.BigDecimal.valueOf(daysInSplit), 2, java.math.RoundingMode.HALF_UP);

                    LocalDate date = splitStart;
                    while (!date.isAfter(splitEnd)) {
                        PlanLineCreateDTO line = new PlanLineCreateDTO();
                        line.setWorkDate(date);
                        line.setPlannedQty(dailyQty);
                        lines.add(line);
                        date = date.plusDays(1);
                    }
                    splitStart = splitEnd.plusDays(1);
                    splitIndex++;
                }
                break;
        }

        return lines;
    }

    /**
     * Compare two plan versions and return comparison details.
     */
    @Transactional(readOnly = true)
    public com.elina.projects.dto.PlanVersionComparisonDTO comparePlanVersions(Long planVersionId1,
            Long planVersionId2) {
        Long tenantId = TenantContext.getTenantId();

        PlanVersion version1 = planVersionRepository.findById(planVersionId1)
                .orElseThrow(() -> new NotFoundException("Plan version 1 not found"));
        PlanVersion version2 = planVersionRepository.findById(planVersionId2)
                .orElseThrow(() -> new NotFoundException("Plan version 2 not found"));

        if (!version1.getTenant().getId().equals(tenantId) || !version2.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("Plan version not found");
        }

        // Get plan lines for both versions
        List<PlanLine> lines1 = planLineRepository.findByPlanVersionId(planVersionId1, true);
        List<PlanLine> lines2 = planLineRepository.findByPlanVersionId(planVersionId2, true);

        // Build comparison
        Map<LocalDate, java.math.BigDecimal> qtyMap1 = lines1.stream()
                .collect(Collectors.toMap(PlanLine::getWorkDate, PlanLine::getPlannedQty, (a, b) -> a));
        Map<LocalDate, java.math.BigDecimal> qtyMap2 = lines2.stream()
                .collect(Collectors.toMap(PlanLine::getWorkDate, PlanLine::getPlannedQty, (a, b) -> a));

        Set<LocalDate> allDates = new java.util.HashSet<>();
        allDates.addAll(qtyMap1.keySet());
        allDates.addAll(qtyMap2.keySet());

        List<com.elina.projects.dto.PlanVersionComparisonDTO.ComparisonLineDTO> comparisonLines = allDates.stream()
                .sorted().map(date -> {
                    java.math.BigDecimal qty1 = qtyMap1.getOrDefault(date, java.math.BigDecimal.ZERO);
                    java.math.BigDecimal qty2 = qtyMap2.getOrDefault(date, java.math.BigDecimal.ZERO);
                    java.math.BigDecimal diff = qty2.subtract(qty1);

                    String status;
                    if (!qtyMap1.containsKey(date)) {
                        status = "NEW";
                    } else if (!qtyMap2.containsKey(date)) {
                        status = "REMOVED";
                    } else if (diff.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        status = "INCREASED";
                    } else if (diff.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        status = "DECREASED";
                    } else {
                        status = "SAME";
                    }

                    return new com.elina.projects.dto.PlanVersionComparisonDTO.ComparisonLineDTO(
                            date, qty1, qty2, diff, status);
                }).collect(Collectors.toList());

        // Build summary
        java.math.BigDecimal totalQty1 = lines1.stream()
                .map(PlanLine::getPlannedQty)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalQty2 = lines2.stream()
                .map(PlanLine::getPlannedQty)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, Integer> changeStats = comparisonLines.stream()
                .collect(Collectors.groupingBy(
                        com.elina.projects.dto.PlanVersionComparisonDTO.ComparisonLineDTO::getStatus,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        long commonDays = comparisonLines.stream()
                .filter(line -> "SAME".equals(line.getStatus()))
                .count();

        com.elina.projects.dto.PlanVersionComparisonDTO.ComparisonSummaryDTO summary = new com.elina.projects.dto.PlanVersionComparisonDTO.ComparisonSummaryDTO(
                lines1.size(),
                lines2.size(),
                (int) commonDays,
                changeStats.getOrDefault("NEW", 0),
                changeStats.getOrDefault("REMOVED", 0),
                totalQty1,
                totalQty2,
                totalQty2.subtract(totalQty1),
                changeStats);

        com.elina.projects.dto.PlanVersionComparisonDTO comparison = new com.elina.projects.dto.PlanVersionComparisonDTO();
        comparison.setVersion1(toDTO(version1));
        comparison.setVersion2(toDTO(version2));
        comparison.setComparisonLines(comparisonLines);
        comparison.setSummary(summary);

        return comparison;
    }
}
