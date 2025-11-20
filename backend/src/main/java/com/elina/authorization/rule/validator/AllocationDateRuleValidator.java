package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Validator for resource allocation date rules.
 * Validates allocation start and end dates.
 */
@Component
public class AllocationDateRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        LocalDate allocationStartDate = context.getAllocationStartDate();
        LocalDate allocationEndDate = context.getAllocationEndDate();

        if (allocationStartDate != null && allocationEndDate != null) {
            if (allocationEndDate.isBefore(allocationStartDate)) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("Allocation end date (%s) cannot be before start date (%s).", 
                        allocationEndDate, allocationStartDate),
                    "Allocation end date must be on or after the start date."
                );
            }
        }

        // Rule 501: Additional validation for allocation dates
        if (rule.getRuleNumber() == 501) {
            if (allocationStartDate == null || allocationEndDate == null) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    "Allocation start date and end date are required.",
                    "Please provide both start and end dates for the allocation."
                );
            }
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{203, 501}; // Rule 203: general allocation date validation, Rule 501: allocation start/end date validation
    }
}

