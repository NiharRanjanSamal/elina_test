package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

/**
 * Validator for confirmation and lock rules:
 * - Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN - Prevents overwriting confirmed entries
 */
@Component
public class ConfirmationLockRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        if (rule.getRuleNumber() == 301) {
            validateConfirmationCannotBeOverwritten(rule, context);
        }
    }

    private void validateConfirmationCannotBeOverwritten(BusinessRule rule, BusinessRuleContext context) {
        if (Boolean.TRUE.equals(context.getIsConfirmed())) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                "Cannot modify confirmed entry. Entry ID: " + context.getEntityId(),
                "Once an entry is confirmed, it cannot be modified. Please contact administrator if changes are required."
            );
        }

        if (Boolean.TRUE.equals(context.getIsLocked())) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                "Cannot modify locked entry. Entry ID: " + context.getEntityId(),
                "This entry has been locked and cannot be modified."
            );
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{301};
    }
}

