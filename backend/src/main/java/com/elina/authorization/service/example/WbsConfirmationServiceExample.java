package com.elina.authorization.service.example;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EXAMPLE SERVICE: WbsConfirmationService
 * 
 * This is an EXAMPLE showing how to validate business rules
 * when confirming a WBS (Work Breakdown Structure).
 * 
 * Rule enforced:
 * - Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN - Prevents modifying confirmed entries
 */
@Service
public class WbsConfirmationServiceExample {

    private static final Logger logger = LoggerFactory.getLogger(WbsConfirmationServiceExample.class);

    private final BusinessRuleEngine businessRuleEngine;
    // private final WbsRepository wbsRepository;

    public WbsConfirmationServiceExample(BusinessRuleEngine businessRuleEngine) {
        this.businessRuleEngine = businessRuleEngine;
    }

    /**
     * EXAMPLE: Confirm WBS with business rule validation.
     * 
     * Validates that the WBS is not already confirmed or locked.
     */
    @Transactional
    public void confirmWbs(Long wbsId) {
        // 1. Fetch WBS
        // Wbs wbs = wbsRepository.findById(wbsId)
        //     .orElseThrow(() -> new RuntimeException("WBS not found"));

        // 2. Build context for rule validation
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("CONFIRMATION")
            .entityId(wbsId)
            .isConfirmed(true) // wbs.getIsConfirmed()
            .isLocked(false) // wbs.getIsLocked()
            .confirmationDate(java.time.LocalDate.now()) // wbs.getConfirmationDate()
            .build();

        // 3. Validate Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
        // This rule prevents confirming an already confirmed or locked entry
        try {
            businessRuleEngine.validate(301, context);
        } catch (BusinessRuleException e) {
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            throw e; // Re-throw for GlobalExceptionHandler
        }

        // 4. Proceed with confirmation if validation passes
        // wbs.setIsConfirmed(true);
        // wbs.setConfirmationDate(LocalDate.now());
        // wbsRepository.save(wbs);
        
        logger.info("WBS {} confirmed successfully", wbsId);
    }

    private Long getCurrentUserId() {
        return 1L; // Placeholder
    }
}

