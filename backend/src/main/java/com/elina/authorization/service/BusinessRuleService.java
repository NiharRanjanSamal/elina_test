package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.dto.BusinessRuleCreateDTO;
import com.elina.authorization.dto.BusinessRuleDTO;
import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.BusinessRuleRepository;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for business rule management with tenant-aware operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Business rules are tenant-specific.
 */
@Service
public class BusinessRuleService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessRuleService.class);

    private final BusinessRuleRepository businessRuleRepository;
    private final TenantRepository tenantRepository;
    private final BusinessRuleEngine businessRuleEngine;

    public BusinessRuleService(BusinessRuleRepository businessRuleRepository,
                              TenantRepository tenantRepository,
                              BusinessRuleEngine businessRuleEngine) {
        this.businessRuleRepository = businessRuleRepository;
        this.tenantRepository = tenantRepository;
        this.businessRuleEngine = businessRuleEngine;
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
    private BusinessRuleDTO toDTO(BusinessRule entity) {
        BusinessRuleDTO dto = new BusinessRuleDTO();
        dto.setRuleId(entity.getRuleId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setRuleNumber(entity.getRuleNumber());
        dto.setControlPoint(entity.getControlPoint());
        dto.setApplicability(entity.getApplicability());
        dto.setRuleValue(entity.getRuleValue());
        dto.setDescription(entity.getDescription());
        dto.setActivateFlag(entity.getActivateFlag());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedOn(entity.getUpdatedOn());
        return dto;
    }

    /**
     * List all business rules for the tenant.
     */
    @Transactional(readOnly = true)
    public List<BusinessRuleDTO> listBusinessRules() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }

        List<BusinessRule> entities = businessRuleRepository.findAllOrderedByRuleNumber();
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get business rule by ID.
     */
    @Transactional(readOnly = true)
    public BusinessRuleDTO getBusinessRule(Long id) {
        Long tenantId = TenantContext.getTenantId();
        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business rule not found"));
        
        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Business rule not found");
        }
        
        return toDTO(entity);
    }

    /**
     * Get business rule by rule number.
     */
    @Transactional(readOnly = true)
    public BusinessRuleDTO getBusinessRuleByNumber(Integer ruleNumber) {
        BusinessRule entity = businessRuleRepository.findByRuleNumber(ruleNumber)
                .orElseThrow(() -> new RuntimeException("Business rule not found"));
        return toDTO(entity);
    }

    /**
     * Create a new business rule.
     */
    @Transactional
    public BusinessRuleDTO createBusinessRule(BusinessRuleCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Check if rule number already exists
        if (businessRuleRepository.existsByRuleNumber(dto.getRuleNumber())) {
            throw new RuntimeException("Business rule with number " + dto.getRuleNumber() + " already exists");
        }

        // Validate applicability
        if (!"Y".equalsIgnoreCase(dto.getApplicability()) && !"N".equalsIgnoreCase(dto.getApplicability())) {
            throw new RuntimeException("Applicability must be Y or N");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        BusinessRule entity = new BusinessRule();
        entity.setTenant(tenant);
        entity.setRuleNumber(dto.getRuleNumber());
        entity.setControlPoint(dto.getControlPoint());
        entity.setApplicability(dto.getApplicability().toUpperCase());
        entity.setRuleValue(dto.getRuleValue());
        entity.setDescription(dto.getDescription());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : true);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

        BusinessRule saved = businessRuleRepository.save(entity);
        
        // Refresh rule engine cache
        businessRuleEngine.refreshCache();
        
        logger.info("Created business rule: {} for tenant {}", dto.getRuleNumber(), tenantId);
        return toDTO(saved);
    }

    /**
     * Update an existing business rule.
     */
    @Transactional
    public BusinessRuleDTO updateBusinessRule(Long id, BusinessRuleCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business rule not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Business rule not found");
        }

        // Check if rule number change would create duplicate
        if (!entity.getRuleNumber().equals(dto.getRuleNumber()) && 
            businessRuleRepository.existsByRuleNumber(dto.getRuleNumber())) {
            throw new RuntimeException("Business rule with number " + dto.getRuleNumber() + " already exists");
        }

        // Validate applicability
        if (!"Y".equalsIgnoreCase(dto.getApplicability()) && !"N".equalsIgnoreCase(dto.getApplicability())) {
            throw new RuntimeException("Applicability must be Y or N");
        }

        entity.setRuleNumber(dto.getRuleNumber());
        entity.setControlPoint(dto.getControlPoint());
        entity.setApplicability(dto.getApplicability().toUpperCase());
        entity.setRuleValue(dto.getRuleValue());
        entity.setDescription(dto.getDescription());
        entity.setActivateFlag(dto.getActivateFlag() != null ? dto.getActivateFlag() : entity.getActivateFlag());
        entity.setUpdatedBy(userId);

        BusinessRule saved = businessRuleRepository.save(entity);
        
        // Refresh rule engine cache
        businessRuleEngine.refreshCache();
        
        logger.info("Updated business rule: {} for tenant {}", dto.getRuleNumber(), tenantId);
        return toDTO(saved);
    }

    /**
     * Delete a business rule.
     */
    @Transactional
    public void deleteBusinessRule(Long id) {
        Long tenantId = TenantContext.getTenantId();

        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business rule not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Business rule not found");
        }

        businessRuleRepository.delete(entity);
        
        // Refresh rule engine cache
        businessRuleEngine.refreshCache();
        
        logger.info("Deleted business rule: {} for tenant {}", id, tenantId);
    }

    /**
     * Toggle activate flag for a business rule.
     */
    @Transactional
    public BusinessRuleDTO toggleActivateFlag(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        BusinessRule entity = businessRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business rule not found"));

        // Verify tenant ownership
        if (!entity.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Business rule not found");
        }

        // Toggle activate flag
        entity.setActivateFlag(!entity.getActivateFlag());
        entity.setUpdatedBy(userId);

        BusinessRule saved = businessRuleRepository.save(entity);
        
        // Refresh rule engine cache
        businessRuleEngine.refreshCache();
        
        logger.info("Toggled activate flag for business rule: {} to {} for tenant {}", 
            id, saved.getActivateFlag(), tenantId);
        return toDTO(saved);
    }

    /**
     * Get all control points.
     */
    @Transactional(readOnly = true)
    public List<String> getAllControlPoints() {
        List<BusinessRule> rules = businessRuleRepository.findAllOrderedByRuleNumber();
        return rules.stream()
                .map(BusinessRule::getControlPoint)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}

