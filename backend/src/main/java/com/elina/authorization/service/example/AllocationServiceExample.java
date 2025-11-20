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
 * EXAMPLE SERVICE: AllocationService
 * 
 * This is an EXAMPLE showing how to validate business rules
 * when creating or updating resource allocations.
 * 
 * Rule enforced:
 * - Rule 501: ALLOCATION_START_END_DATE_MUST_BE_VALID - Validates allocation date ranges
 */
@Service
public class AllocationServiceExample {

    private static final Logger logger = LoggerFactory.getLogger(AllocationServiceExample.class);

    private final BusinessRuleEngine businessRuleEngine;
    // private final AllocationRepository allocationRepository;

    public AllocationServiceExample(BusinessRuleEngine businessRuleEngine) {
        this.businessRuleEngine = businessRuleEngine;
    }

    /**
     * EXAMPLE: Create manpower allocation with business rule validation.
     * 
     * Validates that allocation start and end dates are valid.
     */
    @Transactional
    public void createAllocation(Long resourceId, LocalDate startDate, LocalDate endDate) {
        // 1. Build context for rule validation
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("ALLOCATION")
            .allocationStartDate(startDate)
            .allocationEndDate(endDate)
            .build();

        // 2. Validate Rule 501: ALLOCATION_START_END_DATE_MUST_BE_VALID
        // This rule ensures end date is not before start date
        try {
            businessRuleEngine.validate(501, context);
        } catch (BusinessRuleException e) {
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            throw e; // Re-throw for GlobalExceptionHandler
        }

        // 3. Proceed with allocation creation if validation passes
        // Allocation allocation = new Allocation();
        // allocation.setResourceId(resourceId);
        // allocation.setStartDate(startDate);
        // allocation.setEndDate(endDate);
        // allocationRepository.save(allocation);
        
        logger.info("Allocation created successfully for resource {}", resourceId);
    }

    private Long getCurrentUserId() {
        return 1L; // Placeholder
    }
}

