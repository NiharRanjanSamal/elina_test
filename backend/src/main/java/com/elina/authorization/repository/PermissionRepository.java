package com.elina.authorization.repository;

import com.elina.authorization.entity.Permission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Permission entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 */
@Repository
public interface PermissionRepository extends TenantAwareRepository<Permission, Long> {
    
    @Query("SELECT p FROM Permission p WHERE p.code = :code AND p.tenant.id = :tenantId")
    Optional<Permission> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Permission p WHERE p.tenant.id = :tenantId AND p.isActive = true")
    List<Permission> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);
}

