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
import com.elina.projects.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final PlanVersionRepository planVersionRepository;
    private final PlanLineRepository planLineRepository;
    private final TaskRepository taskRepository;
    private final TenantRepository tenantRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public PlanService(PlanVersionRepository planVersionRepository,
                      PlanLineRepository planLineRepository,
                      TaskRepository taskRepository,
                      TenantRepository tenantRepository,
                      BusinessRuleEngine businessRuleEngine,
                      AuditLogService auditLogService) {
        this.planVersionRepository = planVersionRepository;
        this.planLineRepository = planLineRepository;
        this.taskRepository = taskRepository;
        this.tenantRepository = tenantRepository;
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
        dto.setVersionId(entity.getVersionId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setTaskId(entity.getTask().getTaskId());
        dto.setVersionNumber(entity.getVersionNumber());
        dto.setVersionDate(entity.getVersionDate());
        dto.setDescription(entity.getDescription());
        dto.setIsCurrent(entity.getIsCurrent());
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
        dto.setLineId(entity.getLineId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setVersionId(entity.getPlanVersion().getVersionId());
        dto.setTaskId(entity.getTask().getTaskId());
        dto.setLineNumber(entity.getLineNumber());
        dto.setPlannedDate(entity.getPlannedDate());
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
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Task not found");
        }

        List<PlanVersion> versions = planVersionRepository.findByTaskId(taskId, true);
        return versions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get plan version with lines.
     */
    @Transactional(readOnly = true)
    public PlanVersionDTO getPlanVersion(Long versionId) {
        Long tenantId = TenantContext.getTenantId();

        PlanVersion version = planVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Plan version not found"));
        
        if (!version.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Plan version not found");
        }

        return toDTO(version);
    }

    /**
     * Get plan lines for a version.
     */
    @Transactional(readOnly = true)
    public List<PlanLineDTO> getPlanLines(Long versionId) {
        Long tenantId = TenantContext.getTenantId();

        PlanVersion version = planVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Plan version not found"));
        
        if (!version.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Plan version not found");
        }

        List<PlanLine> lines = planLineRepository.findByVersionId(versionId, true);
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
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        if (!task.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Task not found");
        }

        // Build BusinessRuleContext for validation
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
        Integer maxVersion = planVersionRepository.findMaxVersionNumberByTaskId(dto.getTaskId());
        Integer nextVersionNumber = maxVersion != null ? maxVersion + 1 : 1;

        // Mark all previous versions as not current
        List<PlanVersion> previousVersions = planVersionRepository.findByTaskId(dto.getTaskId(), true);
        for (PlanVersion prevVersion : previousVersions) {
            prevVersion.setIsCurrent(false);
            planVersionRepository.save(prevVersion);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Create plan version
        PlanVersion version = new PlanVersion();
        version.setTenant(tenant);
        version.setTask(task);
        version.setVersionNumber(nextVersionNumber);
        version.setVersionDate(dto.getVersionDate());
        version.setDescription(dto.getDescription());
        version.setIsCurrent(true);
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
            line.setPlannedDate(lineDto.getPlannedDate());
            line.setPlannedQty(lineDto.getPlannedQty());
            line.setDescription(lineDto.getDescription());
            line.setActivateFlag(true);
            line.setCreatedBy(userId);
            line.setUpdatedBy(userId);
            planLineRepository.save(line);
        }
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("versionNumber", saved.getVersionNumber());
        newData.put("versionDate", saved.getVersionDate());
        newData.put("lineCount", dto.getLines().size());
        auditLogService.writeAuditLog("plan_versions", saved.getVersionId(), "INSERT", null, newData);
        
        logger.info("Created plan version: {} for task {} for tenant {}", 
            saved.getVersionNumber(), dto.getTaskId(), tenantId);
        return toDTO(saved);
    }

    /**
     * Revert to a plan version (validate business rules on revert).
     */
    @Transactional
    public PlanVersionDTO revertToPlanVersion(Long versionId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        PlanVersion version = planVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Plan version not found"));
        
        if (!version.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Plan version not found");
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

        // Mark all previous versions as not current
        List<PlanVersion> previousVersions = planVersionRepository.findByTaskId(version.getTask().getTaskId(), true);
        for (PlanVersion prevVersion : previousVersions) {
            prevVersion.setIsCurrent(false);
            planVersionRepository.save(prevVersion);
        }

        // Set this version as current
        version.setIsCurrent(true);
        PlanVersion saved = planVersionRepository.save(version);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("versionNumber", saved.getVersionNumber());
        newData.put("isCurrent", true);
        auditLogService.writeAuditLog("plan_versions", saved.getVersionId(), "UPDATE", 
            Map.of("isCurrent", false), newData);
        
        logger.info("Reverted to plan version: {} for task {} for tenant {}", 
            saved.getVersionNumber(), saved.getTask().getTaskId(), tenantId);
        return toDTO(saved);
    }
}

