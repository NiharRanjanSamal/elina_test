package com.elina.authorization.rule.validator;

import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import com.elina.authorization.entity.BusinessRule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Validator for WBS date range rules.
 * Validates that WBS start and end dates are valid.
 */
@Component
public class WbsDateRangeRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        LocalDate wbsStartDate = context.getWbsStartDate();
        LocalDate wbsEndDate = context.getWbsEndDate();

        if (wbsStartDate != null && wbsEndDate != null) {
            if (wbsEndDate.isBefore(wbsStartDate)) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("WBS end date (%s) cannot be before start date (%s).", wbsEndDate, wbsStartDate),
                    "WBS end date must be on or after the start date."
                );
            }
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{202}; // Example rule number for WBS date validation
    }
}

