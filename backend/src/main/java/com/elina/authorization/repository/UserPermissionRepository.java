package com.elina.authorization.repository;

import com.elina.authorization.entity.UserPermission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserPermission entity with tenant-aware queries.
 * 
 * Tenant enforcement: Queries filter by user's tenant_id to ensure tenant isolation.
 */
@Repository
public interface UserPermissionRepository extends TenantAwareRepository<UserPermission, Long> {
    
    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId AND up.user.tenant.id = :tenantId")
    List<UserPermission> findByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId AND up.user.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()}")
    List<UserPermission> findByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId AND up.permission.id = :permissionId AND up.user.tenant.id = :tenantId")
    Optional<UserPermission> findByUserIdAndPermissionId(@Param("userId") Long userId, @Param("permissionId") Long permissionId, @Param("tenantId") Long tenantId);

    void deleteByUserIdAndPermissionId(Long userId, Long permissionId);
}

