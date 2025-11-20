package com.elina.authorization.rule;

import com.elina.authorization.entity.BusinessRule;

/**
 * Interface for business rule validators.
 * 
 * Each validator implements specific rule validation logic.
 * Validators are registered in BusinessRuleEngine by rule number.
 */
public interface BusinessRuleValidator {

    /**
     * Validate the business rule against the provided context.
     * 
     * @param rule The business rule to validate
     * @param context The context containing data to validate
     * @throws BusinessRuleException if the rule is violated
     */
    void validate(BusinessRule rule, BusinessRuleContext context) throws BusinessRuleException;

    /**
     * Get the rule numbers this validator handles.
     * 
     * @return Array of rule numbers (e.g., [101, 102])
     */
    int[] getSupportedRuleNumbers();
}

