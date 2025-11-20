package com.elina.authorization.repository;

import com.elina.authorization.entity.BusinessRule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BusinessRule entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * This ensures business rules from one tenant cannot access rules from another tenant.
 */
@Repository
public interface BusinessRuleRepository extends TenantAwareRepository<BusinessRule, Long> {
    
    /**
     * Find business rule by tenant and rule number.
     * Uses SpEL to retrieve tenantId from TenantContext.
     */
    @Query("SELECT br FROM BusinessRule br WHERE br.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND br.ruleNumber = :ruleNumber")
    Optional<BusinessRule> findByRuleNumber(@Param("ruleNumber") Integer ruleNumber);

    /**
     * Find all active and applicable business rules for tenant.
     */
    @Query("SELECT br FROM BusinessRule br WHERE br.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND br.activateFlag = true AND br.applicability = 'Y'")
    List<BusinessRule> findAllActiveAndApplicable();

    /**
     * Find business rules by control point.
     */
    @Query("SELECT br FROM BusinessRule br WHERE br.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND br.controlPoint = :controlPoint AND br.activateFlag = true AND br.applicability = 'Y'")
    List<BusinessRule> findByControlPoint(@Param("controlPoint") String controlPoint);

    /**
     * Find all business rules for tenant (including inactive).
     */
    @Query("SELECT br FROM BusinessRule br WHERE br.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "ORDER BY br.ruleNumber")
    List<BusinessRule> findAllOrderedByRuleNumber();

    /**
     * Check if rule number exists for tenant.
     */
    @Query("SELECT COUNT(br) > 0 FROM BusinessRule br WHERE br.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND br.ruleNumber = :ruleNumber")
    boolean existsByRuleNumber(@Param("ruleNumber") Integer ruleNumber);
}

