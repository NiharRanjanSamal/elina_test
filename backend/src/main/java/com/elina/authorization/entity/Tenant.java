package com.elina.authorization.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tenant entity representing a multi-tenant organization.
 * 
 * Tenant enforcement: All user operations are filtered by tenant_id.
 * The TenantContext (ThreadLocal) is set by TenantFilter to ensure
 * tenant isolation at the application layer.
 */
@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", unique = true, nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "client_code", length = 50)
    private String clientCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

