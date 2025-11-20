package com.elina.authorization.repository;

import com.elina.authorization.entity.RolePermission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RolePermission entity with tenant-aware queries.
 * 
 * Tenant enforcement: Queries filter by role's tenant_id to ensure tenant isolation.
 */
@Repository
public interface RolePermissionRepository extends TenantAwareRepository<RolePermission, Long> {
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenant.id = :tenantId")
    List<RolePermission> findByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.role.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()}")
    List<RolePermission> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId AND rp.role.tenant.id = :tenantId")
    Optional<RolePermission> findByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId, @Param("tenantId") Long tenantId);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}

