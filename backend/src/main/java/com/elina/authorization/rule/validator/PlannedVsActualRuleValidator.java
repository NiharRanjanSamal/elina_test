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

        // Calculate new actual quantity after update
        BigDecimal newActualQty = actualQty != null ? actualQty : BigDecimal.ZERO;
        if (dailyUpdateQty != null) {
            newActualQty = newActualQty.add(dailyUpdateQty);
        } else if (updateQty != null) {
            newActualQty = updateQty;
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

