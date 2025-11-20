package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.ProjectCreateDTO;
import com.elina.projects.dto.ProjectDTO;
import com.elina.projects.dto.ProjectDetailsDTO;
import com.elina.projects.entity.Project;
import com.elina.projects.repository.ProjectRepository;
import com.elina.projects.repository.WbsRepository;
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
 * Service for project management with tenant-aware operations and business rule validation.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id from TenantContext.
 * Projects are tenant-specific.
 * 
 * Business rules validated:
 * - Rule 201: START_DATE_CANNOT_BE_IN_FUTURE - Project start_date cannot be in future
 * - Rule 202: WBS_DATE_RANGE_VALIDATION - Project end_date must be after start_date
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final WbsRepository wbsRepository;
    private final TenantRepository tenantRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public ProjectService(ProjectRepository projectRepository,
                         WbsRepository wbsRepository,
                         TenantRepository tenantRepository,
                         BusinessRuleEngine businessRuleEngine,
                         AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.wbsRepository = wbsRepository;
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
     * Convert entity to DTO.
     */
    private ProjectDTO toDTO(Project entity) {
        ProjectDTO dto = new ProjectDTO();
        dto.setProjectId(entity.getProjectId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setProjectCode(entity.getProjectCode());
        dto.setProjectName(entity.getProjectName());
        dto.setDescription(entity.getDescription());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * List projects with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<ProjectDTO> listProjects(String search, Boolean activeOnly, int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Project> entities = projectRepository.findWithFilters(activeOnly, search, pageable);
        return entities.map(this::toDTO);
    }

    /**
     * Get project by ID.
     */
    @Transactional(readOnly = true)
    public ProjectDTO getProject(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Project entity = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }
        
        return toDTO(entity);
    }

    /**
     * Get project details with aggregated summary.
     */
    @Transactional(readOnly = true)
    public ProjectDetailsDTO getProjectDetails(Long projectId) {
        ProjectDTO project = getProject(projectId);
        
        // Get WBS count
        List<com.elina.projects.entity.Wbs> wbsList = wbsRepository.findByProjectId(projectId, true);
        int wbsCount = wbsList.size();
        
        // Calculate totals (simplified - would need task aggregation)
        java.math.BigDecimal totalPlannedQty = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalActualQty = java.math.BigDecimal.ZERO;
        int taskCount = 0;
        
        // Build WBS hierarchy
        List<com.elina.projects.dto.WbsDTO> rootWbs = wbsRepository.findRootWbsByProjectId(projectId, true)
                .stream()
                .map(this::buildWbsHierarchy)
                .collect(Collectors.toList());
        
        ProjectDetailsDTO details = new ProjectDetailsDTO();
        details.setProject(project);
        details.setTotalWbsCount(wbsCount);
        details.setTotalTaskCount(taskCount);
        details.setTotalPlannedQty(totalPlannedQty);
        details.setTotalActualQty(totalActualQty);
        details.setWbsHierarchy(rootWbs);
        
        return details;
    }

    /**
     * Build WBS hierarchy recursively.
     */
    private com.elina.projects.dto.WbsDTO buildWbsHierarchy(com.elina.projects.entity.Wbs wbs) {
        com.elina.projects.dto.WbsDTO dto = toWbsDTO(wbs);
        
        List<com.elina.projects.entity.Wbs> children = wbsRepository.findByParentWbsId(wbs.getWbsId(), true);
        List<com.elina.projects.dto.WbsDTO> childDtos = children.stream()
                .map(this::buildWbsHierarchy)
                .collect(Collectors.toList());
        
        dto.setChildren(childDtos);
        return dto;
    }

    /**
     * Convert WBS entity to DTO (helper method).
     */
    private com.elina.projects.dto.WbsDTO toWbsDTO(com.elina.projects.entity.Wbs entity) {
        com.elina.projects.dto.WbsDTO dto = new com.elina.projects.dto.WbsDTO();
        dto.setWbsId(entity.getWbsId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setProjectId(entity.getProject().getProjectId());
        dto.setParentWbsId(entity.getParentWbs() != null ? entity.getParentWbs().getWbsId() : null);
        dto.setWbsCode(entity.getWbsCode());
        dto.setWbsName(entity.getWbsName());
        dto.setDescription(entity.getDescription());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setLevel(entity.getLevel());
        dto.setWorkCenter(entity.getWorkCenter());
        dto.setCostCenter(entity.getCostCenter());
        dto.setPlannedQty(entity.getPlannedQty());
        dto.setActualQty(entity.getActualQty());
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
     * Create a new project.
     */
    @Transactional
    public ProjectDTO createProject(ProjectCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Check if project code already exists
        if (projectRepository.existsByProjectCode(dto.getProjectCode())) {
            throw new RuntimeException("Project with code " + dto.getProjectCode() + " already exists");
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("PROJECT")
                .taskStartDate(dto.getStartDate()) // Reuse taskStartDate field for project start date
                .taskEndDate(dto.getEndDate()) // Reuse taskEndDate field for project end date
                .wbsStartDate(dto.getStartDate()) // Also set for date range validation
                .wbsEndDate(dto.getEndDate())
                .build();

        // Validate business rules
        // Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
        if (dto.getStartDate() != null) {
            try {
                businessRuleEngine.validate(201, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        // Rule 202: WBS_DATE_RANGE_VALIDATION (applies to projects too - end_date must be after start_date)
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BusinessRuleException(202, 
                    "Project end date (" + dto.getEndDate() + ") cannot be before start date (" + dto.getStartDate() + ").",
                    "Project end date must be on or after the start date.");
            }
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Project entity = new Project();
        entity.setTenant(tenant);
        entity.setProjectCode(dto.getProjectCode());
        entity.setProjectName(dto.getProjectName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        Project saved = projectRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("projectCode", saved.getProjectCode());
        newData.put("projectName", saved.getProjectName());
        newData.put("startDate", saved.getStartDate());
        newData.put("endDate", saved.getEndDate());
        auditLogService.writeAuditLog("projects", saved.getProjectId(), "INSERT", null, newData);
        
        logger.info("Created project: {} for tenant {}", dto.getProjectCode(), tenantId);
        return toDTO(saved);
    }

    /**
     * Update an existing project.
     */
    @Transactional
    public ProjectDTO updateProject(Long id, ProjectCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Project entity = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }

        // Check if project code change would create duplicate
        if (!entity.getProjectCode().equals(dto.getProjectCode()) && 
            projectRepository.existsByProjectCode(dto.getProjectCode())) {
            throw new RuntimeException("Project with code " + dto.getProjectCode() + " already exists");
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("PROJECT")
                .entityId(id)
                .taskStartDate(dto.getStartDate())
                .taskEndDate(dto.getEndDate())
                .wbsStartDate(dto.getStartDate())
                .wbsEndDate(dto.getEndDate())
                .build();

        // Validate business rules
        if (dto.getStartDate() != null) {
            try {
                businessRuleEngine.validate(201, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BusinessRuleException(202,
                    "Project end date (" + dto.getEndDate() + ") cannot be before start date (" + dto.getStartDate() + ").",
                    "Project end date must be on or after the start date.");
            }
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("projectCode", entity.getProjectCode());
        oldData.put("projectName", entity.getProjectName());
        oldData.put("startDate", entity.getStartDate());
        oldData.put("endDate", entity.getEndDate());
        oldData.put("status", entity.getStatus());

        entity.setProjectCode(dto.getProjectCode());
        entity.setProjectName(dto.getProjectName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : entity.getStatus());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : entity.getActivateFlag());
        entity.setUpdatedBy(userId);

        Project saved = projectRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("projectCode", saved.getProjectCode());
        newData.put("projectName", saved.getProjectName());
        newData.put("startDate", saved.getStartDate());
        newData.put("endDate", saved.getEndDate());
        newData.put("status", saved.getStatus());
        auditLogService.writeAuditLog("projects", saved.getProjectId(), "UPDATE", oldData, newData);
        
        logger.info("Updated project: {} for tenant {}", dto.getProjectCode(), tenantId);
        return toDTO(saved);
    }

    /**
     * Delete a project (soft delete - set activateFlag to false).
     */
    @Transactional
    public void deleteProject(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Project entity = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("projectCode", entity.getProjectCode());
        oldData.put("projectName", entity.getProjectName());
        oldData.put("activateFlag", entity.getActivateFlag());

        entity.setActivateFlag(false);
        projectRepository.save(entity);
        
        // Write audit log
        auditLogService.writeAuditLog("projects", id, "DELETE", oldData, null);
        
        logger.info("Deleted project: {} for tenant {}", id, tenantId);
    }
}

