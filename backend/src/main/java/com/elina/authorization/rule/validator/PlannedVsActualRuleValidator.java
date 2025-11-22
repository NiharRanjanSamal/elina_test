package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validator for planned vs actual quantity rules:
 * - Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY - Prevents daily updates from exceeding planned quantity
 */
@Component
public class PlannedVsActualRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        if (rule.getRuleNumber() == 401) {
            validateDailyUpdateCannotExceedPlannedQty(rule, context);
        }
    }

    private void validateDailyUpdateCannotExceedPlannedQty(BusinessRule rule, BusinessRuleContext context) {
        BigDecimal plannedQty = context.getPlannedQty();
        BigDecimal dailyUpdateQty = context.getDailyUpdateQty();
        BigDecimal actualQty = context.getActualQty();
        BigDecimal updateQty = context.getUpdateQty();

        if (plannedQty == null) {
            return; // No planned quantity to validate against
        }

        // For Rule 401: Check if the actual quantity (final value) exceeds planned quantity
        // actualQty in context is already the NEW value the user wants to set
        // We should validate the actualQty directly, not add dailyUpdateQty to it
        BigDecimal newActualQty;
        if (actualQty != null) {
            // actualQty is the final value - use it directly
            newActualQty = actualQty;
        } else if (updateQty != null) {
            // updateQty is an alternative field for the final value
            newActualQty = updateQty;
        } else if (dailyUpdateQty != null) {
            // If only dailyUpdateQty is provided (incremental update), we'd need existing actualQty
            // But in our case, actualQty should always be provided for task updates
            newActualQty = dailyUpdateQty;
        } else {
            return; // No quantity to validate
        }

        if (newActualQty.compareTo(plannedQty) > 0) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                String.format("Daily update quantity (%.2f) cannot exceed planned quantity (%.2f).", 
                    newActualQty, plannedQty),
                String.format("The total actual quantity (%.2f) exceeds the planned quantity (%.2f). " +
                    "Please adjust the update quantity.", newActualQty, plannedQty)
            );
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{401};
    }
}

