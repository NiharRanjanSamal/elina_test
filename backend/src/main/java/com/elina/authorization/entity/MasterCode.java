package com.elina.authorization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Master Code entity for tenant-aware master data / configuration management.
 * 
 * Tenant enforcement: Master codes belong to a specific tenant.
 * All queries must include tenant_id filter via @TenantAware base service.
 * The tenant_id is included in JWT claims for request-level validation.
 * 
 * This table stores configuration codes that can be used across the system
 * for dropdowns, radio buttons, and other selection components.
 */
@Entity
@Table(name = "master_codes", indexes = {
    @Index(name = "idx_master_codes_tenant_type", columnList = "tenant_id,code_type"),
    @Index(name = "idx_master_codes_tenant_type_active", columnList = "tenant_id,code_type,activate_flag"),
    @Index(name = "idx_master_codes_code_type", columnList = "code_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long codeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "code_type", nullable = false, length = 100)
    private String codeType;

    @Column(name = "code_value", nullable = false, length = 255)
    private String codeValue;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "long_description", columnDefinition = "NVARCHAR(MAX)")
    private String longDescription;

    @Column(name = "activate_flag", nullable = false)
    private Boolean activateFlag = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
        if (updatedOn == null) {
            updatedOn = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
    }
}

