package com.elina.authorization.repository;

import com.elina.authorization.entity.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserRole entity with tenant-aware queries.
 * 
 * Tenant enforcement: Queries filter by user's tenant_id to ensure tenant isolation.
 */
@Repository
public interface UserRoleRepository extends TenantAwareRepository<UserRole, Long> {
    
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.user.tenant.id = :tenantId")
    List<UserRole> findByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.user.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()}")
    List<UserRole> findByUserId(@Param("userId") Long userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId AND ur.user.tenant.id = :tenantId")
    Optional<UserRole> findByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}

