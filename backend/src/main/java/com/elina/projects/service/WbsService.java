package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.service.UserAuthorizationService;
import com.elina.projects.dto.WbsCreateDTO;
import com.elina.projects.dto.WbsDTO;
import com.elina.projects.entity.Project;
import com.elina.projects.entity.Wbs;
import com.elina.projects.repository.ProjectRepository;
import com.elina.projects.repository.WbsRepository;
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
 * Service for WBS management with tenant-aware operations and business rule validation.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id from TenantContext.
 * WBS are tenant-specific and support hierarchical structure.
 * 
 * Business rules validated:
 * - Rule 202: WBS_DATE_RANGE_VALIDATION - WBS end_date cannot be before start_date
 * - Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN - Confirmed WBS cannot be modified
 * 
 * Object-level authorization:
 * - Checks user authorizations for work_center/cost_center
 * - Filters WBS based on user's authorized work centers and cost centers
 */
@Service
public class WbsService {

    private static final Logger logger = LoggerFactory.getLogger(WbsService.class);

    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;
    private final UserAuthorizationService userAuthorizationService;

    public WbsService(WbsRepository wbsRepository,
                     ProjectRepository projectRepository,
                     TenantRepository tenantRepository,
                     BusinessRuleEngine businessRuleEngine,
                     AuditLogService auditLogService,
                     UserAuthorizationService userAuthorizationService) {
        this.wbsRepository = wbsRepository;
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.businessRuleEngine = businessRuleEngine;
        this.auditLogService = auditLogService;
        this.userAuthorizationService = userAuthorizationService;
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
    private WbsDTO toDTO(Wbs entity) {
        WbsDTO dto = new WbsDTO();
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
     * Check if user is authorized for work center/cost center.
     * Returns true if user has authorization or if work_center/cost_center is not set.
     */
    private boolean isUserAuthorized(Long userId, String workCenter, String costCenter) {
        if (workCenter == null && costCenter == null) {
            return true; // No restriction
        }
        
        try {
            List<com.elina.authorization.entity.UserAuthorization> authorizations = 
                userAuthorizationService.findByUserId(userId);
            
            return authorizations.stream()
                .filter(ua -> ua.getIsAllowed() != null && ua.getIsAllowed())
                .anyMatch(ua -> {
                    if ("WORK_CENTER".equals(ua.getResourceType()) && workCenter != null) {
                        return workCenter.equals(ua.getResourceId());
                    }
                    if ("COST_CENTER".equals(ua.getResourceType()) && costCenter != null) {
                        return costCenter.equals(ua.getResourceId());
                    }
                    return false;
                });
        } catch (Exception e) {
            logger.warn("Error checking user authorization", e);
            return false;
        }
    }

    /**
     * Get WBS hierarchy for a project (with authorization filtering).
     */
    @Transactional(readOnly = true)
    public List<WbsDTO> getWbsHierarchy(Long projectId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Verify project belongs to tenant
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }

        // Get root WBS
        List<Wbs> rootWbs = wbsRepository.findRootWbsByProjectId(projectId, true);
        
        // Filter by authorization and build hierarchy
        return rootWbs.stream()
                .filter(wbs -> isUserAuthorized(userId, wbs.getWorkCenter(), wbs.getCostCenter()))
                .map(wbs -> buildWbsHierarchy(wbs, userId))
                .collect(Collectors.toList());
    }

    /**
     * Build WBS hierarchy recursively.
     */
    private WbsDTO buildWbsHierarchy(Wbs wbs, Long userId) {
        WbsDTO dto = toDTO(wbs);
        
        List<Wbs> children = wbsRepository.findByParentWbsId(wbs.getWbsId(), true);
        List<WbsDTO> childDtos = children.stream()
                .filter(child -> isUserAuthorized(userId, child.getWorkCenter(), child.getCostCenter()))
                .map(child -> buildWbsHierarchy(child, userId))
                .collect(Collectors.toList());
        
        dto.setChildren(childDtos);
        return dto;
    }

    /**
     * Get all WBS for a project.
     */
    @Transactional(readOnly = true)
    public List<WbsDTO> getWbsByProjectId(Long projectId) {
        Long tenantId = TenantContext.getTenantId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }

        List<Wbs> wbsList = wbsRepository.findByProjectId(projectId, true);
        return wbsList.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get WBS by ID.
     */
    @Transactional(readOnly = true)
    public WbsDTO getWbs(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Wbs entity = wbsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WBS not found"));
        
        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("WBS not found");
        }

        // Check authorization
        if (!isUserAuthorized(userId, entity.getWorkCenter(), entity.getCostCenter())) {
            throw new RuntimeException("WBS not found"); // Don't reveal existence
        }
        
        return toDTO(entity);
    }

    /**
     * Create a new WBS.
     */
    @Transactional
    public WbsDTO createWbs(WbsCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Verify project belongs to tenant
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Project not found");
        }

        // Check authorization
        if (!isUserAuthorized(userId, dto.getWorkCenter(), dto.getCostCenter())) {
            throw new RuntimeException("Unauthorized to create WBS for this work center/cost center");
        }

        // Check if WBS code already exists
        if (wbsRepository.existsByWbsCode(dto.getWbsCode())) {
            throw new RuntimeException("WBS with code " + dto.getWbsCode() + " already exists");
        }

        // Validate dates against parent WBS if parent exists
        if (dto.getParentWbsId() != null) {
            Wbs parentWbs = wbsRepository.findById(dto.getParentWbsId())
                    .orElseThrow(() -> new RuntimeException("Parent WBS not found"));
            
            if (!parentWbs.getTenant().getId().equals(tenantId)) {
                throw new RuntimeException("Parent WBS not found");
            }

            if (dto.getStartDate() != null && parentWbs.getStartDate() != null) {
                if (dto.getStartDate().isBefore(parentWbs.getStartDate())) {
                    throw new BusinessRuleException(202,
                        "WBS start date (" + dto.getStartDate() + ") cannot be before parent WBS start date (" + parentWbs.getStartDate() + ").",
                        "WBS start date must be on or after parent WBS start date.");
                }
            }

            if (dto.getEndDate() != null && parentWbs.getEndDate() != null) {
                if (dto.getEndDate().isAfter(parentWbs.getEndDate())) {
                    throw new BusinessRuleException(202,
                        "WBS end date (" + dto.getEndDate() + ") cannot be after parent WBS end date (" + parentWbs.getEndDate() + ").",
                        "WBS end date must be on or before parent WBS end date.");
                }
            }
        }

        // Build BusinessRuleContext for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("WBS")
                .wbsStartDate(dto.getStartDate())
                .wbsEndDate(dto.getEndDate())
                .build();

        // Validate Rule 202: WBS_DATE_RANGE_VALIDATION
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            try {
                businessRuleEngine.validate(202, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Wbs entity = new Wbs();
        entity.setTenant(tenant);
        entity.setProject(project);
        entity.setParentWbs(dto.getParentWbsId() != null ? 
            wbsRepository.findById(dto.getParentWbsId()).orElse(null) : null);
        entity.setWbsCode(dto.getWbsCode());
        entity.setWbsName(dto.getWbsName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setWorkCenter(dto.getWorkCenter());
        entity.setCostCenter(dto.getCostCenter());
        entity.setPlannedQty(dto.getPlannedQty());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "PLANNED");
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        Wbs saved = wbsRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("wbsCode", saved.getWbsCode());
        newData.put("wbsName", saved.getWbsName());
        newData.put("startDate", saved.getStartDate());
        newData.put("endDate", saved.getEndDate());
        auditLogService.writeAuditLog("wbs", saved.getWbsId(), "INSERT", null, newData);
        
        logger.info("Created WBS: {} for tenant {}", dto.getWbsCode(), tenantId);
        return toDTO(saved);
    }

    /**
     * Update an existing WBS.
     */
    @Transactional
    public WbsDTO updateWbs(Long id, WbsCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Wbs entity = wbsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WBS not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("WBS not found");
        }

        // Check authorization
        if (!isUserAuthorized(userId, entity.getWorkCenter(), entity.getCostCenter())) {
            throw new RuntimeException("Unauthorized to update this WBS");
        }

        // Validate Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
        if (entity.getIsConfirmed() != null && entity.getIsConfirmed()) {
            BusinessRuleContext context = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("WBS")
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

        // Check if WBS code change would create duplicate
        if (!entity.getWbsCode().equals(dto.getWbsCode()) && 
            wbsRepository.existsByWbsCode(dto.getWbsCode())) {
            throw new RuntimeException("WBS with code " + dto.getWbsCode() + " already exists");
        }

        // Build BusinessRuleContext for date validation
        BusinessRuleContext context = BusinessRuleContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("WBS")
                .entityId(id)
                .wbsStartDate(dto.getStartDate())
                .wbsEndDate(dto.getEndDate())
                .build();

        // Validate Rule 202: WBS_DATE_RANGE_VALIDATION
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            try {
                businessRuleEngine.validate(202, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("wbsCode", entity.getWbsCode());
        oldData.put("wbsName", entity.getWbsName());
        oldData.put("startDate", entity.getStartDate());
        oldData.put("endDate", entity.getEndDate());
        oldData.put("status", entity.getStatus());

        entity.setWbsCode(dto.getWbsCode());
        entity.setWbsName(dto.getWbsName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setWorkCenter(dto.getWorkCenter());
        entity.setCostCenter(dto.getCostCenter());
        entity.setPlannedQty(dto.getPlannedQty());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : entity.getStatus());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : entity.getActivateFlag());
        entity.setUpdatedBy(userId);

        Wbs saved = wbsRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("wbsCode", saved.getWbsCode());
        newData.put("wbsName", saved.getWbsName());
        newData.put("startDate", saved.getStartDate());
        newData.put("endDate", saved.getEndDate());
        newData.put("status", saved.getStatus());
        auditLogService.writeAuditLog("wbs", saved.getWbsId(), "UPDATE", oldData, newData);
        
        logger.info("Updated WBS: {} for tenant {}", dto.getWbsCode(), tenantId);
        return toDTO(saved);
    }

    /**
     * Move WBS to different parent.
     */
    @Transactional
    public WbsDTO moveWbs(Long id, Long newParentWbsId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Wbs entity = wbsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WBS not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("WBS not found");
        }

        // Check authorization
        if (!isUserAuthorized(userId, entity.getWorkCenter(), entity.getCostCenter())) {
            throw new RuntimeException("Unauthorized to move this WBS");
        }

        // Validate Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
        if (entity.getIsConfirmed() != null && entity.getIsConfirmed()) {
            BusinessRuleContext context = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("WBS")
                    .entityId(id)
                    .isConfirmed(true)
                    .build();
            
            try {
                businessRuleEngine.validate(301, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        Wbs newParent = null;
        if (newParentWbsId != null) {
            newParent = wbsRepository.findById(newParentWbsId)
                    .orElseThrow(() -> new RuntimeException("Parent WBS not found"));
            
            if (!newParent.getTenant().getId().equals(tenantId)) {
                throw new RuntimeException("Parent WBS not found");
            }
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("parentWbsId", entity.getParentWbs() != null ? entity.getParentWbs().getWbsId() : null);

        entity.setParentWbs(newParent);

        Wbs saved = wbsRepository.save(entity);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("parentWbsId", saved.getParentWbs() != null ? saved.getParentWbs().getWbsId() : null);
        auditLogService.writeAuditLog("wbs", saved.getWbsId(), "UPDATE", oldData, newData);
        
        logger.info("Moved WBS: {} to parent {}", saved.getWbsCode(), newParentWbsId);
        return toDTO(saved);
    }

    /**
     * Compute WBS planned and confirmed qty (aggregate from tasks).
     */
    @Transactional(readOnly = true)
    public WbsDTO computeWbsPlannedAndConfirmedQty(Long wbsId) {
        // This would aggregate from tasks - simplified for now
        WbsDTO dto = getWbs(wbsId);
        // TODO: Aggregate from tasks
        return dto;
    }

    /**
     * Delete a WBS (soft delete - set activateFlag to false).
     */
    @Transactional
    public void deleteWbs(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Wbs entity = wbsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WBS not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("WBS not found");
        }

        // Check authorization
        if (!isUserAuthorized(userId, entity.getWorkCenter(), entity.getCostCenter())) {
            throw new RuntimeException("Unauthorized to delete this WBS");
        }

        // Prepare old data for audit
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("wbsCode", entity.getWbsCode());
        oldData.put("activateFlag", entity.getActivateFlag());

        entity.setActivateFlag(false);
        wbsRepository.save(entity);
        
        // Write audit log
        auditLogService.writeAuditLog("wbs", id, "DELETE", oldData, null);
        
        logger.info("Deleted WBS: {} for tenant {}", id, tenantId);
    }
}

