package com.elina.authorization.context;

/**
 * TenantContext uses ThreadLocal to store tenant information per request thread.
 * 
 * Tenant enforcement: This ensures all operations within a request are scoped to a single tenant.
 * The TenantFilter extracts tenant_id from JWT and sets it here, which is then used by
 * @TenantAware services to automatically filter queries.
 * 
 * To reuse in other systems: Copy this class and ensure TenantFilter sets it from JWT/request.
 */
public class TenantContext {

    private static final ThreadLocal<Long> tenantId = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this request thread.
     * Called by TenantFilter after JWT validation.
     */
    public static void setTenantId(Long id) {
        tenantId.set(id);
    }

    /**
     * Get the current tenant ID for this request thread.
     * Returns null if not set (should only happen in unauthenticated requests).
     */
    public static Long getTenantId() {
        return tenantId.get();
    }

    /**
     * Clear the tenant context for this thread.
     * Called by TenantFilter after request processing to prevent memory leaks.
     */
    public static void clear() {
        tenantId.remove();
    }
}

