package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.ConfirmationCreateDTO;
import com.elina.projects.dto.ConfirmationDTO;
import com.elina.projects.entity.Confirmation;
import com.elina.projects.entity.Task;
import com.elina.projects.entity.Wbs;
import com.elina.projects.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for confirmation management with tenant-aware operations and business rule validation.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id from TenantContext.
 * Confirmations are tenant-specific.
 * 
 * Business rules validated:
 * - Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN - Entity must not be already confirmed
 */
@Service
public class ConfirmationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmationService.class);

    private final ConfirmationRepository confirmationRepository;
    private final WbsRepository wbsRepository;
    private final TaskRepository taskRepository;
    private final TenantRepository tenantRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public ConfirmationService(ConfirmationRepository confirmationRepository,
                              WbsRepository wbsRepository,
                              TaskRepository taskRepository,
                              TenantRepository tenantRepository,
                              BusinessRuleEngine businessRuleEngine,
                              AuditLogService auditLogService) {
        this.confirmationRepository = confirmationRepository;
        this.wbsRepository = wbsRepository;
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
     * Convert entity to DTO.
     */
    private ConfirmationDTO toDTO(Confirmation entity) {
        ConfirmationDTO dto = new ConfirmationDTO();
        dto.setConfirmationId(entity.getConfirmationId());
        dto.setTenantId(entity.getTenant().getId());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setConfirmationDate(entity.getConfirmationDate());
        dto.setConfirmedBy(entity.getConfirmedBy());
        dto.setConfirmedOn(entity.getConfirmedOn());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        return dto;
    }

    /**
     * Confirm a WBS or Task.
     */
    @Transactional
    public ConfirmationDTO confirmEntity(ConfirmationCreateDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        // Check if already confirmed
        if (confirmationRepository.existsByEntityTypeAndEntityId(dto.getEntityType(), dto.getEntityId())) {
            // Build BusinessRuleContext for validation
            BusinessRuleContext context = BusinessRuleContext.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .entityType("CONFIRMATION")
                    .entityId(dto.getEntityId())
                    .confirmationDate(dto.getConfirmationDate())
                    .isConfirmed(true)
                    .build();

            // Validate Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
            try {
                businessRuleEngine.validate(301, context);
            } catch (BusinessRuleException e) {
                logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
                throw e;
            }
        }

        // Verify entity exists and belongs to tenant
        if ("WBS".equals(dto.getEntityType())) {
            Wbs wbs = wbsRepository.findById(dto.getEntityId())
                    .orElseThrow(() -> new RuntimeException("WBS not found"));
            
            if (!wbs.getTenant().getId().equals(tenantId)) {
                throw new RuntimeException("WBS not found");
            }

            // Mark WBS as confirmed
            wbs.setIsConfirmed(true);
            wbs.setConfirmedOn(LocalDateTime.now());
            wbs.setConfirmedBy(userId);
            wbsRepository.save(wbs);
        } else if ("TASK".equals(dto.getEntityType())) {
            Task task = taskRepository.findById(dto.getEntityId())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            
            if (!task.getTenant().getId().equals(tenantId)) {
                throw new RuntimeException("Task not found");
            }

            // Mark Task as confirmed
            task.setIsConfirmed(true);
            task.setConfirmedOn(LocalDateTime.now());
            task.setConfirmedBy(userId);
            taskRepository.save(task);
        } else {
            throw new RuntimeException("Invalid entity type: " + dto.getEntityType());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Create confirmation record
        Confirmation confirmation = new Confirmation();
        confirmation.setTenant(tenant);
        confirmation.setEntityType(dto.getEntityType());
        confirmation.setEntityId(dto.getEntityId());
        confirmation.setConfirmationDate(dto.getConfirmationDate());
        confirmation.setConfirmedBy(userId);
        confirmation.setRemarks(dto.getRemarks());
        confirmation.setCreatedBy(userId);

        Confirmation saved = confirmationRepository.save(confirmation);
        
        // Write audit log
        Map<String, Object> newData = new HashMap<>();
        newData.put("entityType", saved.getEntityType());
        newData.put("entityId", saved.getEntityId());
        newData.put("confirmationDate", saved.getConfirmationDate());
        auditLogService.writeAuditLog("confirmations", saved.getConfirmationId(), "INSERT", null, newData);
        
        logger.info("Confirmed {} {} for tenant {}", dto.getEntityType(), dto.getEntityId(), tenantId);
        return toDTO(saved);
    }
}

