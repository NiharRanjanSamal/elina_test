package com.elina.authorization.repository;

import com.elina.authorization.entity.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Role entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 */
@Repository
public interface RoleRepository extends TenantAwareRepository<Role, Long> {
    
    @Query("SELECT r FROM Role r WHERE r.code = :code AND r.tenant.id = :tenantId")
    Optional<Role> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT r FROM Role r WHERE r.tenant.id = :tenantId AND r.isActive = true")
    List<Role> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);
}

