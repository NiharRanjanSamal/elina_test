package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Validator for task date range rules:
 * - Rule 201: START_DATE_CANNOT_BE_IN_FUTURE - Task start date cannot be in the future
 */
@Component
public class TaskDateRangeRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        if (rule.getRuleNumber() == 201) {
            validateStartDateCannotBeInFuture(rule, context);
        }
    }

    private void validateStartDateCannotBeInFuture(BusinessRule rule, BusinessRuleContext context) {
        LocalDate today = LocalDate.now();
        LocalDate taskStartDate = context.getTaskStartDate();

        if (taskStartDate != null && taskStartDate.isAfter(today)) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                String.format("Task start date (%s) cannot be in the future.", taskStartDate),
                "Task start date must be today or in the past."
            );
        }

        // Also validate end date is after start date
        LocalDate taskEndDate = context.getTaskEndDate();
        if (taskStartDate != null && taskEndDate != null && taskEndDate.isBefore(taskStartDate)) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                String.format("Task end date (%s) cannot be before start date (%s).", taskEndDate, taskStartDate),
                "End date must be on or after the start date."
            );
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{201};
    }
}

