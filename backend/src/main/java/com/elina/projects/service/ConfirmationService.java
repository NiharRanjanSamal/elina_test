package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.dto.WbsConfirmationRequest;
import com.elina.projects.dto.WbsConfirmationResponse;
import com.elina.projects.dto.WbsConfirmationSummaryDTO;
import com.elina.projects.entity.ConfirmationEntity;
import com.elina.projects.entity.ConfirmationLockEntity;
import com.elina.projects.entity.Wbs;
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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Confirmation and locking orchestration service.
 *
 * Responsibilities:
 * - Validates rules before freezing WBS progress
 * - Maintains confirmation_locks table
 * - Produces summary + history views
 * - Handles admin undo (rule-gated)
 */
@Service
public class ConfirmationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmationService.class);
    private static final Integer RULE_BACKDATE_AFTER_LOCK = 102;
    private static final Integer RULE_NO_FUTURE = 201;
    private static final Integer RULE_NOT_BEFORE_TASK_START = 601;
    private static final Integer RULE_NON_EMPTY = 602;
    private static final Integer RULE_UNDO_WINDOW = 603;

    private final ConfirmationRepository confirmationRepository;
    private final ConfirmationLockRepository confirmationLockRepository;
    private final WbsRepository wbsRepository;
    private final TaskRepository taskRepository;
    private final TaskUpdateRepository taskUpdateRepository;
    private final TenantRepository tenantRepository;
    private final BusinessRuleEngine businessRuleEngine;
    private final AuditLogService auditLogService;

    public ConfirmationService(ConfirmationRepository confirmationRepository,
                               ConfirmationLockRepository confirmationLockRepository,
                               WbsRepository wbsRepository,
                               TaskRepository taskRepository,
                               TaskUpdateRepository taskUpdateRepository,
                               TenantRepository tenantRepository,
                               BusinessRuleEngine businessRuleEngine,
                               AuditLogService auditLogService) {
        this.confirmationRepository = confirmationRepository;
        this.confirmationLockRepository = confirmationLockRepository;
        this.wbsRepository = wbsRepository;
        this.taskRepository = taskRepository;
        this.taskUpdateRepository = taskUpdateRepository;
        this.tenantRepository = tenantRepository;
        this.businessRuleEngine = businessRuleEngine;
        this.auditLogService = auditLogService;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long principal) {
            return principal;
        }
        return null;
    }

    private Wbs getWbsOrThrow(Long wbsId, Long tenantId) {
        Wbs wbs = wbsRepository.findById(wbsId)
            .orElseThrow(() -> new NotFoundException("WBS not found"));
        if (!wbs.getTenant().getId().equals(tenantId)) {
            throw new NotFoundException("WBS not found");
        }
        return wbs;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Map<String, Object> confirmationAuditPayload(ConfirmationEntity entity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("entityType", entity.getEntityType());
        payload.put("entityId", entity.getEntityId());
        payload.put("confirmationDate", entity.getConfirmationDate());
        payload.put("remarks", entity.getRemarks());
        return payload;
    }

    private WbsConfirmationResponse toResponse(ConfirmationEntity entity, LocalDate lockDate) {
        // Fetch WBS if entity type is WBS
        Wbs wbs = null;
        if ("WBS".equals(entity.getEntityType())) {
            wbs = wbsRepository.findById(entity.getEntityId()).orElse(null);
        }
        
        return WbsConfirmationResponse.builder()
            .confirmationId(entity.getConfirmationId())
            .wbsId(wbs != null ? wbs.getWbsId() : entity.getEntityId())
            .wbsCode(wbs != null ? wbs.getWbsCode() : null)
            .wbsName(wbs != null ? wbs.getWbsName() : null)
            .confirmationDate(entity.getConfirmationDate())
            .confirmedQty(BigDecimal.ZERO) // Not stored in database, calculate if needed
            .remarks(entity.getRemarks())
            .createdBy(entity.getCreatedBy())
            .createdOn(entity.getCreatedOn())
            .lockDate(lockDate)
            .build();
    }

    @Transactional
    public WbsConfirmationSummaryDTO confirmWbs(Long wbsId, WbsConfirmationRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();
        Wbs wbs = getWbsOrThrow(wbsId, tenantId);

        LocalDate confirmationDate = request.getConfirmationDate();
        ConfirmationLockEntity existingLock = confirmationLockRepository.findByWbsId(wbsId).orElse(null);
        LocalDate lockDate = existingLock != null ? existingLock.getLockDate() : null;
        LocalDate earliestTaskStart = taskRepository.findEarliestTaskStartDateForWbs(wbsId);
        LocalDate baselineStart = earliestTaskStart != null ? earliestTaskStart : wbs.getStartDate();
        BigDecimal confirmedQty = taskUpdateRepository.sumActualQtyForWbsAndDate(wbsId, confirmationDate);

        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(tenantId)
            .userId(userId)
            .entityType("WBS_CONFIRMATION")
            .entityId(wbsId)
            .confirmationDate(confirmationDate)
            .lockDate(lockDate)
            .wbsStartDate(baselineStart)
            .actualQty(confirmedQty)
            .build();

        try {
            businessRuleEngine.validateAll(
                Arrays.asList(
                    RULE_BACKDATE_AFTER_LOCK,
                    RULE_NO_FUTURE,
                    RULE_NOT_BEFORE_TASK_START,
                    RULE_NON_EMPTY
                ),
                context
            );
        } catch (BusinessRuleException ex) {
            logger.warn("Confirmation blocked by rule {} - {}", ex.getRuleNumber(), ex.getMessage());
            throw ex;
        }

        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        ConfirmationEntity entity = new ConfirmationEntity();
        entity.setTenant(tenant);
        entity.setEntityType("WBS");
        entity.setEntityId(wbs.getWbsId());
        entity.setConfirmationDate(confirmationDate);
        entity.setConfirmedBy(userId);
        entity.setConfirmedOn(java.time.LocalDateTime.now());
        entity.setRemarks(request.getRemarks());
        entity.setCreatedBy(userId);

        ConfirmationEntity saved = confirmationRepository.save(entity);

        auditLogService.writeAuditLog(
            "CONFIRMATIONS",
            saved.getConfirmationId(),
            "INSERT",
            null,
            confirmationAuditPayload(saved)
        );

        updateLockRecord(wbs, tenant, existingLock, confirmationDate, userId);

        logger.info("WBS {} confirmed for date {} by user {}", wbsId, confirmationDate, userId);
        return getConfirmationSummary(wbsId, null);
    }

    @Transactional(readOnly = true)
    public List<WbsConfirmationResponse> getWbsConfirmations(Long wbsId) {
        Long tenantId = TenantContext.getTenantId();
        Wbs wbs = getWbsOrThrow(wbsId, tenantId);
        LocalDate lockDate = confirmationLockRepository.findLockDateByWbsId(wbsId);

        return confirmationRepository.findByWbsIdOrderByConfirmationDateDesc(wbsId).stream()
            .map(entity -> toResponse(entity, lockDate))
            .toList();
    }

    @Transactional(readOnly = true)
    public WbsConfirmationSummaryDTO getConfirmationSummary(Long wbsId, LocalDate previewDate) {
        Long tenantId = TenantContext.getTenantId();
        Wbs wbs = getWbsOrThrow(wbsId, tenantId);
        LocalDate lockDate = confirmationLockRepository.findLockDateByWbsId(wbsId);
        LocalDate lastConfirmationDate = confirmationRepository.findLatestConfirmationDateForWbs(wbsId);
        BigDecimal confirmedQtyToDate = confirmationRepository.sumConfirmedQtyByWbs(wbsId);
        BigDecimal plannedQty = wbs.getPlannedQty();
        if (plannedQty == null) {
            plannedQty = taskRepository.sumPlannedQtyByWbsId(wbsId);
        }
        BigDecimal actualQty = wbs.getActualQty();
        if (actualQty == null) {
            actualQty = taskRepository.sumActualQtyByWbsId(wbsId);
        }
        BigDecimal variance = defaultZero(actualQty).subtract(defaultZero(confirmedQtyToDate));
        BigDecimal previewActualQty = null;
        if (previewDate != null) {
            previewActualQty = taskUpdateRepository.sumActualQtyForWbsAndDate(wbsId, previewDate);
        }

        return WbsConfirmationSummaryDTO.builder()
            .wbsId(wbs.getWbsId())
            .wbsCode(wbs.getWbsCode())
            .wbsName(wbs.getWbsName())
            .lastConfirmationDate(lastConfirmationDate)
            .lockDate(lockDate)
            .plannedQty(defaultZero(plannedQty))
            .actualQty(defaultZero(actualQty))
            .confirmedQtyToDate(defaultZero(confirmedQtyToDate))
            .variance(variance)
            .previewDate(previewDate)
            .previewActualQty(previewActualQty)
            .build();
    }

    @Transactional
    public WbsConfirmationSummaryDTO undoConfirmation(Long confirmationId) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();
        ConfirmationEntity confirmation = confirmationRepository.findByIdForTenant(confirmationId)
            .orElseThrow(() -> new NotFoundException("Confirmation not found"));
        Wbs wbs = getWbsOrThrow(confirmation.getEntityId(), tenantId);

        enforceUndoRule(confirmation.getConfirmationDate());

        Map<String, Object> oldData = confirmationAuditPayload(confirmation);
        confirmationRepository.delete(confirmation);
        auditLogService.writeAuditLog(
            "CONFIRMATIONS",
            confirmationId,
            "DELETE",
            oldData,
            null
        );

        Long wbsId = wbs.getWbsId();
        LocalDate latestDate = confirmationRepository.findLatestConfirmationDateForWbs(wbsId);
        ConfirmationLockEntity lock = confirmationLockRepository.findByWbsId(wbsId).orElse(null);

        if (latestDate == null) {
            if (lock != null) {
                confirmationLockRepository.delete(lock);
                auditLogService.writeAuditLog(
                    "CONFIRMATION_LOCKS",
                    lock.getLockId(),
                    "DELETE",
                    Map.of("wbsId", wbsId, "lockDate", lock.getLockDate()),
                    null
                );
            }
            wbs.setIsLocked(false);
            wbs.setLockDate(null);
        } else {
            Tenant wbsTenant = wbs.getTenant();
            updateLockRecord(wbs, wbsTenant, lock, latestDate, userId);
        }

        wbsRepository.save(wbs);
        logger.info("Undo confirmation {} completed by {}", confirmationId, userId);
        return getConfirmationSummary(wbs.getWbsId(), null);
    }

    private void enforceUndoRule(LocalDate confirmationDate) {
        String ruleValue = businessRuleEngine.getRuleValue(RULE_UNDO_WINDOW);
        if (ruleValue == null) {
            throw new BusinessRuleException(
                RULE_UNDO_WINDOW,
                "Undo is disabled by business rules.",
                "Activate rule 603 (CONFIRMATION_UNDO_WINDOW) with allowed days to enable undo."
            );
        }
        try {
            int allowedDays = Integer.parseInt(ruleValue.trim());
            long days = ChronoUnit.DAYS.between(confirmationDate, LocalDate.now());
            if (days > allowedDays) {
                throw new BusinessRuleException(
                    RULE_UNDO_WINDOW,
                    String.format("Undo window of %d days exceeded. Confirmation date: %s", allowedDays, confirmationDate),
                    "Increase the allowed undo window or request admin override."
                );
            }
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException(
                RULE_UNDO_WINDOW,
                "Invalid rule value for undo window: " + ruleValue,
                "Update rule 603 with a numeric value representing allowed days."
            );
        }
    }

    private void updateLockRecord(Wbs wbs,
                                  Tenant tenant,
                                  ConfirmationLockEntity existingLock,
                                  LocalDate newLockDate,
                                  Long userId) {
        boolean lockChanged = false;
        LocalDate effectiveLockDate = newLockDate;

        if (existingLock == null) {
            ConfirmationLockEntity lockEntity = new ConfirmationLockEntity();
            lockEntity.setTenant(tenant);
            lockEntity.setWbs(wbs);
            lockEntity.setLockDate(newLockDate);
            lockEntity.setCreatedBy(userId);
            ConfirmationLockEntity saved = confirmationLockRepository.save(lockEntity);
            auditLogService.writeAuditLog(
                "CONFIRMATION_LOCKS",
                saved.getLockId(),
                "INSERT",
                null,
                Map.of("wbsId", wbs.getWbsId(), "lockDate", newLockDate)
            );
            lockChanged = true;
        } else if (newLockDate.isAfter(existingLock.getLockDate())) {
            Map<String, Object> oldData = Map.of("lockDate", existingLock.getLockDate());
            existingLock.setLockDate(newLockDate);
            ConfirmationLockEntity saved = confirmationLockRepository.save(existingLock);
            auditLogService.writeAuditLog(
                "CONFIRMATION_LOCKS",
                saved.getLockId(),
                "UPDATE",
                oldData,
                Map.of("lockDate", newLockDate)
            );
            lockChanged = true;
        } else {
            effectiveLockDate = existingLock.getLockDate();
        }

        if (lockChanged || effectiveLockDate != null) {
            wbs.setIsLocked(true);
            wbs.setLockDate(effectiveLockDate);
            wbsRepository.save(wbs);
        }
    }
}

