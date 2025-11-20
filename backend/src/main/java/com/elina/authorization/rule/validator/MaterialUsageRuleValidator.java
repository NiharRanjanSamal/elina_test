package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.authorization.rule.BusinessRuleValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validator for material usage rules.
 * Validates material usage quantities and dates.
 */
@Component
public class MaterialUsageRuleValidator implements BusinessRuleValidator {

    @Override
    public void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException {
        LocalDate materialUsageDate = context.getMaterialUsageDate();
        LocalDate today = LocalDate.now();

        if (materialUsageDate != null) {
            // Check if material usage date is in the future
            if (materialUsageDate.isAfter(today)) {
                throw new BusinessRuleException(
                    rule.getRuleNumber(),
                    String.format("Material usage date (%s) cannot be in the future.", materialUsageDate),
                    "Material usage can only be recorded for today or past dates."
                );
            }
        }

        // Validate material usage quantity
        BigDecimal updateQty = context.getUpdateQty();
        if (updateQty != null && updateQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                rule.getRuleNumber(),
                "Material usage quantity cannot be negative: " + updateQty,
                "Material usage quantity must be zero or positive."
            );
        }
    }

    @Override
    public int[] getSupportedRuleNumbers() {
        return new int[]{205}; // Example rule number for material usage validation
    }
}

