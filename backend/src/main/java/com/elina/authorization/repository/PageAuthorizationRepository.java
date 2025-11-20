package com.elina.authorization.repository;

import com.elina.authorization.entity.PageAuthorization;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PageAuthorization entity with tenant-aware queries.
 * 
 * Tenant enforcement: Queries filter by role/user's tenant_id to ensure tenant isolation.
 */
@Repository
public interface PageAuthorizationRepository extends TenantAwareRepository<PageAuthorization, Long> {
    
    @Query("SELECT pa FROM PageAuthorization pa WHERE pa.pagePath = :pagePath " +
           "AND (pa.role.tenant.id = :tenantId OR pa.user.tenant.id = :tenantId)")
    List<PageAuthorization> findByPagePathAndTenantId(@Param("pagePath") String pagePath, @Param("tenantId") Long tenantId);

    @Query("SELECT pa FROM PageAuthorization pa WHERE pa.role.id = :roleId AND pa.role.tenant.id = :tenantId")
    List<PageAuthorization> findByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Query("SELECT pa FROM PageAuthorization pa WHERE pa.user.id = :userId AND pa.user.tenant.id = :tenantId")
    List<PageAuthorization> findByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}

