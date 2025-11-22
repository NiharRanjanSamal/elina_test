package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Confirmation specific rule validations:
 * 601 - confirmation date must not be earlier than earliest task start
 * 602 - confirmation qty cannot be empty
 */
@Component
public class ConfirmationDateRuleValidator implements BusinessRuleValidator {

    private static final int RULE_NOT_BEFORE_START = 601;
    private static final int RULE_NON_EMPTY = 602;

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        if (rule.getRuleNumber() == RULE_NOT_BEFORE_START) {
            validateNotBeforeEarliestTask(rule, context);
        } else if (rule.getRuleNumber() == RULE_NON_EMPTY) {
            validateNonEmptyQty(rule, context);
        }
    }

    private void validateNotBeforeEarliestTask(BusinessRule rule, BusinessRuleContext context) {
        LocalDate confirmationDate = context.getConfirmationDate();
        LocalDate earliestStart = context.getWbsStartDate();
        if (confirmationDate != null && earliestStart != null && confirmationDate.isBefore(earliestStart)) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                String.format("Confirmation date %s cannot be earlier than earliest task start %s", confirmationDate, earliestStart),
                "Select a confirmation date on or after the earliest task start date."
            );
        }
    }

    private void validateNonEmptyQty(BusinessRule rule, BusinessRuleContext context) {
        BigDecimal confirmedQty = context.getActualQty();
        if (confirmedQty == null || confirmedQty.signum() <= 0) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                "No actual quantity recorded for the selected confirmation date.",
                "Capture task updates for the date before confirming."
            );
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{RULE_NOT_BEFORE_START, RULE_NON_EMPTY};
    }
}


