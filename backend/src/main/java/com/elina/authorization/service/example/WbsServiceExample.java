package com.elina.authorization.service.example;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * EXAMPLE SERVICE: WbsService
 * 
 * This is an EXAMPLE showing how to validate business rules
 * when creating or updating WBS (Work Breakdown Structure).
 * 
 * Rule enforced:
 * - Rule 202: WBS_DATE_RANGE_VALIDATION - Validates WBS end date is not before start date
 */
@Service
public class WbsServiceExample {

    private static final Logger logger = LoggerFactory.getLogger(WbsServiceExample.class);

    private final BusinessRuleEngine businessRuleEngine;
    // private final WbsRepository wbsRepository;

    public WbsServiceExample(BusinessRuleEngine businessRuleEngine) {
        this.businessRuleEngine = businessRuleEngine;
    }

    /**
     * EXAMPLE: Create or update WBS with business rule validation.
     * 
     * Validates that WBS end date is not before start date.
     */
    @Transactional
    public void createOrUpdateWbs(Long wbsId, LocalDate startDate, LocalDate endDate) {
        // 1. Build context for rule validation
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("WBS")
            .entityId(wbsId)
            .wbsStartDate(startDate)
            .wbsEndDate(endDate)
            .build();

        // 2. Validate Rule 202: WBS_DATE_RANGE_VALIDATION
        // This rule ensures WBS end date is not before start date
        try {
            businessRuleEngine.validate(202, context);
        } catch (BusinessRuleException e) {
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            throw e; // Re-throw for GlobalExceptionHandler
        }

        // 3. Proceed with WBS creation/update if validation passes
        // Wbs wbs = wbsId != null ? wbsRepository.findById(wbsId).orElse(new Wbs()) : new Wbs();
        // wbs.setStartDate(startDate);
        // wbs.setEndDate(endDate);
        // wbsRepository.save(wbs);
        
        logger.info("WBS {} created/updated successfully", wbsId);
    }

    private Long getCurrentUserId() {
        return 1L; // Placeholder
    }
}

