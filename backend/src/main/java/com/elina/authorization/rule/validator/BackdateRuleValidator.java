package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Validator for backdate rules:
 * - Rule 101: BACKDATE_ALLOWED_TILL - Maximum days allowed for backdating
 * - Rule 102: BACKDATE_ALLOWED_AFTER_LOCK - Whether backdating is allowed after lock date
 */
@Component
public class BackdateRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        LocalDate today = LocalDate.now();
        LocalDate updateDate = context.getUpdateDate();
        LocalDate confirmationDate = context.getConfirmationDate();
        LocalDate lockDate = context.getLockDate();

        if (updateDate == null && confirmationDate == null) {
            return; // No date to validate
        }

        LocalDate dateToCheck = updateDate != null ? updateDate : confirmationDate;

        if (rule.getRuleNumber() == 101) {
            // BACKDATE_ALLOWED_TILL: Check if backdating exceeds allowed days
            validateBackdateAllowedTill(rule, dateToCheck, today);
        } else if (rule.getRuleNumber() == 102) {
            // BACKDATE_ALLOWED_AFTER_LOCK: Check if backdating is allowed after lock date
            validateBackdateAfterLock(rule, dateToCheck, lockDate, today);
        }
    }

    private void validateBackdateAllowedTill(BusinessRule rule, LocalDate dateToCheck, LocalDate today) {
        if (dateToCheck.isAfter(today)) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                "Cannot backdate to a future date: " + dateToCheck,
                "Update date cannot be in the future."
            );
        }

        if (dateToCheck.isBefore(today)) {
            // This is a backdate - check if it's within allowed limit
            try {
                int allowedDays = Integer.parseInt(rule.getRuleValue());
                long daysDifference = ChronoUnit.DAYS.between(dateToCheck, today);

                if (daysDifference > allowedDays) {
                    throw new BusinessRuleException(
                        rule.getRuleNumber(),
                        String.format("Backdating is only allowed for %d days. Attempted to backdate by %d days.", 
                            allowedDays, daysDifference),
                        String.format("You can only backdate up to %d days from today.", allowedDays)
                    );
                }
            } catch (NumberFormatException e) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    "Invalid rule value for BACKDATE_ALLOWED_TILL: " + rule.getRuleValue(),
                    "Please contact administrator to configure this rule correctly."
                );
            }
        }
    }

    private void validateBackdateAfterLock(BusinessRule rule, LocalDate dateToCheck, LocalDate lockDate, LocalDate today) {
        if (lockDate == null) {
            return; // No lock date, rule doesn't apply
        }

        if (dateToCheck.isBefore(lockDate)) {
            // Attempting to backdate before lock date
            if (!"Y".equalsIgnoreCase(rule.getRuleValue())) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("Cannot backdate before lock date (%s). Update date: %s", lockDate, dateToCheck),
                    "Backdating before the lock date is not allowed."
                );
            }
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{101, 102};
    }
}

