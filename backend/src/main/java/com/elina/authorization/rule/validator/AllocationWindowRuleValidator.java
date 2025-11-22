package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Ensures allocation windows stay within the WBS period.
 * Rule 601 — ALLOCATION_WITHIN_WBS_DATES_ONLY
 */
@Component
public class AllocationWindowRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        LocalDate wbsStart = context.getWbsStartDate();
        LocalDate wbsEnd = context.getWbsEndDate();
        LocalDate allocationStart = context.getAllocationStartDate();
        LocalDate allocationEnd = context.getAllocationEndDate();

        if (allocationStart == null || allocationEnd == null) {
            return;
        }

        if (wbsStart != null && allocationStart.isBefore(wbsStart)) {
            throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("Allocation start date %s cannot be before WBS start date %s", allocationStart, wbsStart),
                    "Adjust the allocation start date to fall within the WBS duration.");
        }

        if (wbsEnd != null && allocationEnd.isAfter(wbsEnd)) {
            throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("Allocation end date %s cannot be after WBS end date %s", allocationEnd, wbsEnd),
                    "Adjust the allocation end date to fall within the WBS duration.");
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{601};
    }
}

