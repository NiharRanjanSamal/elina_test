package com.elina.authorization.repository;

import com.elina.authorization.entity.MasterCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MasterCode entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * This ensures master codes from one tenant cannot access codes from another tenant.
 */
@Repository
public interface MasterCodeRepository extends TenantAwareRepository<MasterCode, Long> {
    
    /**
     * Find master codes by tenant and code type.
     * Uses SpEL to retrieve tenantId from TenantContext.
     */
    @Query("SELECT mc FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND mc.codeType = :codeType")
    List<MasterCode> findByCodeType(@Param("codeType") String codeType);

    /**
     * Find active master codes by tenant and code type.
     */
    @Query("SELECT mc FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND mc.codeType = :codeType AND mc.activateFlag = true")
    List<MasterCode> findActiveByCodeType(@Param("codeType") String codeType);

    /**
     * Find master code by tenant, code type, and code value.
     */
    @Query("SELECT mc FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND mc.codeType = :codeType AND mc.codeValue = :codeValue")
    Optional<MasterCode> findByCodeTypeAndCodeValue(@Param("codeType") String codeType, @Param("codeValue") String codeValue);

    /**
     * Find master codes with pagination and filtering.
     */
    @Query("SELECT mc FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:codeType IS NULL OR mc.codeType = :codeType) " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR mc.activateFlag = true) " +
           "AND (:search IS NULL OR LOWER(mc.codeValue) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(mc.shortDescription) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MasterCode> findWithFilters(
        @Param("codeType") String codeType,
        @Param("activeOnly") Boolean activeOnly,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Count active codes by code type.
     */
    @Query("SELECT COUNT(mc) FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND mc.codeType = :codeType AND mc.activateFlag = true")
    long countActiveByCodeType(@Param("codeType") String codeType);

    /**
     * Check if code exists for tenant, code type, and code value.
     */
    @Query("SELECT COUNT(mc) > 0 FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND mc.codeType = :codeType AND mc.codeValue = :codeValue")
    boolean existsByCodeTypeAndCodeValue(@Param("codeType") String codeType, @Param("codeValue") String codeValue);

    /**
     * Find all distinct code types for the tenant.
     */
    @Query("SELECT DISTINCT mc.codeType FROM MasterCode mc WHERE mc.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} ORDER BY mc.codeType")
    List<String> findDistinctCodeTypes();
}

