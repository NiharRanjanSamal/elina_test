package com.elina.authorization.repository;

import com.elina.authorization.entity.UserAuthorization;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserAuthorization entity with tenant-aware queries.
 * 
 * Tenant enforcement: Queries filter by user's tenant_id to ensure tenant isolation.
 */
@Repository
public interface UserAuthorizationRepository extends TenantAwareRepository<UserAuthorization, Long> {
    
    @Query("SELECT ua FROM UserAuthorization ua WHERE ua.user.id = :userId AND ua.user.tenant.id = :tenantId")
    List<UserAuthorization> findByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Query("SELECT ua FROM UserAuthorization ua WHERE ua.user.id = :userId AND ua.resourceType = :resourceType AND ua.user.tenant.id = :tenantId")
    List<UserAuthorization> findByUserIdAndResourceType(@Param("userId") Long userId, @Param("resourceType") String resourceType, @Param("tenantId") Long tenantId);

    @Query("SELECT ua FROM UserAuthorization ua WHERE ua.user.id = :userId AND ua.resourceType = :resourceType AND ua.resourceId = :resourceId AND ua.user.tenant.id = :tenantId")
    Optional<UserAuthorization> findByUserIdAndResource(@Param("userId") Long userId, @Param("resourceType") String resourceType, @Param("resourceId") String resourceId, @Param("tenantId") Long tenantId);
}

