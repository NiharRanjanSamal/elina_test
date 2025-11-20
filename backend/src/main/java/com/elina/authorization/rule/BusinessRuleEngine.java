package com.elina.authorization.rule;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.repository.BusinessRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core Business Rule Engine that validates business rules.
 * 
 * This engine:
 * - Loads active rules for the tenant
 * - Routes validation requests to appropriate validators
 * - Caches rules for performance
 * - Throws BusinessRuleException when rules are violated
 * 
 * Usage:
 *   BusinessRuleEngine engine = ...;
 *   BusinessRuleContext context = BusinessRuleContext.builder()...build();
 *   engine.validate(101, context); // Validates rule 101
 */
@Component
public class BusinessRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(BusinessRuleEngine.class);

    private final BusinessRuleRepository businessRuleRepository;
    private final List<BusinessRuleValidator> validators;
    
    // Rule registry: ruleNumber -> validator
    private final Map<Integer, BusinessRuleValidator> validatorRegistry = new ConcurrentHashMap<>();
    
    // Cache: tenantId -> (ruleNumber -> BusinessRule)
    private final Map<Long, Map<Integer, BusinessRule>> ruleCache = new ConcurrentHashMap<>();

    public BusinessRuleEngine(BusinessRuleRepository businessRuleRepository, 
                             List<BusinessRuleValidator> validators) {
        this.businessRuleRepository = businessRuleRepository;
        this.validators = validators != null ? validators : new ArrayList<>();
    }

    @PostConstruct
    public void initialize() {
        // Register all validators
        for (BusinessRuleValidator validator : validators) {
            for (int ruleNumber : validator.getSupportedRuleNumbers()) {
                validatorRegistry.put(ruleNumber, validator);
                logger.debug("Registered validator for rule {}: {}", ruleNumber, validator.getClass().getSimpleName());
            }
        }
        logger.info("BusinessRuleEngine initialized with {} validators for {} rules", 
            validators.size(), validatorRegistry.size());
    }

    /**
     * Check if a rule is active for the current tenant.
     */
    public boolean isRuleActive(Integer ruleNumber) {
        BusinessRule rule = getRule(ruleNumber);
        return rule != null && rule.isApplicableAndActive();
    }

    /**
     * Get the rule value for a given rule number.
     * Returns null if rule is not found or not active.
     */
    public String getRuleValue(Integer ruleNumber) {
        BusinessRule rule = getRule(ruleNumber);
        if (rule != null && rule.isApplicableAndActive()) {
            return rule.getRuleValue();
        }
        return null;
    }

    /**
     * Validate a business rule against the provided context.
     * 
     * @param ruleNumber The rule number to validate
     * @param context The context containing data to validate
     * @throws BusinessRuleException if the rule is violated
     */
    public void validate(Integer ruleNumber, BusinessRuleContext context) throws BusinessRuleException {
        BusinessRule rule = getRule(ruleNumber);
        
        if (rule == null) {
            logger.debug("Rule {} not found for tenant {}, skipping validation", 
                ruleNumber, context.getTenantId());
            return; // Rule doesn't exist, no validation needed
        }

        if (!rule.isApplicableAndActive()) {
            logger.debug("Rule {} is not applicable or not active, skipping validation", ruleNumber);
            return; // Rule is not applicable or inactive
        }

        // Get validator for this rule
        BusinessRuleValidator validator = validatorRegistry.get(ruleNumber);
        if (validator == null) {
            logger.warn("No validator found for rule {}. Rule exists but cannot be validated.", ruleNumber);
            return; // No validator, skip validation
        }

        // Validate
        try {
            validator.validate(rule, context);
            logger.debug("Rule {} validation passed", ruleNumber);
        } catch (BusinessRuleException e) {
            logger.warn("Rule {} validation failed: {}", ruleNumber, e.getMessage());
            throw e;
        }
    }

    /**
     * Validate multiple rules at once.
     * Stops at first violation.
     */
    public void validateAll(List<Integer> ruleNumbers, BusinessRuleContext context) throws BusinessRuleException {
        for (Integer ruleNumber : ruleNumbers) {
            validate(ruleNumber, context);
        }
    }

    /**
     * Get business rule by number for current tenant.
     */
    private BusinessRule getRule(Integer ruleNumber) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            logger.warn("TenantContext not set, cannot retrieve rule {}", ruleNumber);
            return null;
        }

        // Check cache first
        Map<Integer, BusinessRule> tenantRules = ruleCache.get(tenantId);
        if (tenantRules != null && tenantRules.containsKey(ruleNumber)) {
            return tenantRules.get(ruleNumber);
        }

        // Load from database
        Optional<BusinessRule> ruleOpt = businessRuleRepository.findByRuleNumber(ruleNumber);
        if (ruleOpt.isPresent()) {
            BusinessRule rule = ruleOpt.get();
            // Update cache
            ruleCache.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(ruleNumber, rule);
            return rule;
        }

        return null;
    }

    /**
     * Refresh rule cache for current tenant.
     * Call this after rules are created/updated/deleted.
     */
    public void refreshCache() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            ruleCache.remove(tenantId);
            logger.info("Rule cache refreshed for tenant {}", tenantId);
        }
    }

    /**
     * Get all active rules for current tenant.
     */
    public List<BusinessRule> getAllActiveRules() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Collections.emptyList();
        }

        // Check cache
        Map<Integer, BusinessRule> tenantRules = ruleCache.get(tenantId);
        if (tenantRules != null && !tenantRules.isEmpty()) {
            return tenantRules.values().stream()
                .filter(BusinessRule::isApplicableAndActive)
                .collect(Collectors.toList());
        }

        // Load from database
        List<BusinessRule> rules = businessRuleRepository.findAllActiveAndApplicable();
        
        // Update cache
        Map<Integer, BusinessRule> cache = new ConcurrentHashMap<>();
        for (BusinessRule rule : rules) {
            cache.put(rule.getRuleNumber(), rule);
        }
        ruleCache.put(tenantId, cache);

        return rules;
    }

    /**
     * Get rules by control point for current tenant.
     */
    public List<BusinessRule> getRulesByControlPoint(String controlPoint) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Collections.emptyList();
        }

        return businessRuleRepository.findByControlPoint(controlPoint);
    }
}

