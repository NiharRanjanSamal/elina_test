package com.elina.authorization.repository;

import com.elina.authorization.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Tenant entity.
 * Note: Tenant repository doesn't need tenant filtering since tenants themselves
 * are not tenant-scoped - they are the root of the tenant hierarchy.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantCode(String tenantCode);
    boolean existsByTenantCode(String tenantCode);
}

