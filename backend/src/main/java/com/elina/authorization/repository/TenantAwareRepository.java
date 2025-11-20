package com.elina.authorization.repository;

import com.elina.authorization.context.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository interface with tenant-aware operations.
 * 
 * Tenant enforcement: All repositories extending this interface should
 * automatically filter by tenant_id using TenantContext.getTenantId().
 * This ensures tenant isolation at the data access layer.
 * 
 * To reuse in other systems: Copy this interface and ensure your repository
 * implementations use TenantContext to filter queries.
 * 
 * @param <T> Entity type
 * @param <ID> ID type
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    /**
     * Helper method to get current tenant ID from context.
     * 
     * Tenant enforcement: This is used by repositories to filter queries.
     * If TenantContext is not set, throws IllegalStateException to prevent
     * accidental cross-tenant data access.
     */
    default Long getCurrentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set. Tenant-aware operations require authenticated request.");
        }
        return tenantId;
    }
}

