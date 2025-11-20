package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Validator for attendance entry rules.
 * Validates attendance date and related constraints.
 */
@Component
public class AttendanceEntryRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        LocalDate attendanceDate = context.getAttendanceDate();
        LocalDate today = LocalDate.now();

        if (attendanceDate != null) {
            // Rule 204 and 601: Check if attendance date is in the future
            if (attendanceDate.isAfter(today)) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("Attendance date (%s) cannot be in the future.", attendanceDate),
                    "Attendance can only be posted for today or past dates."
                );
            }
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{204, 601}; // Rule 204: general attendance validation, Rule 601: attendance date cannot be in future
    }
}

