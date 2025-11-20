package com.elina.authorization.repository;

import com.elina.authorization.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * This ensures users from one tenant cannot access users from another tenant.
 */
@Repository
public interface UserRepository extends TenantAwareRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email)) AND u.tenant.id = :tenantId")
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.tenant.tenantCode = :tenantCode")
    Optional<User> findByEmailAndTenantCode(@Param("email") String email, @Param("tenantCode") String tenantCode);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.tenant.id = :tenantId")
    Optional<User> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT u FROM User u JOIN FETCH u.tenant WHERE u.tenant.id = :tenantId")
    java.util.List<User> findByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);
}

