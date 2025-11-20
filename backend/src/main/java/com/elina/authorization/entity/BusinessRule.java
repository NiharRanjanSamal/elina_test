package com.elina.authorization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Business Rule entity for tenant-aware business rule management.
 * 
 * Business rules are ACTIVE VALIDATORS that prevent actions that violate rules.
 * They are not passive - they actively block invalid operations.
 * 
 * Tenant enforcement: Business rules belong to a specific tenant.
 * All queries must include tenant_id filter via @TenantAware base service.
 * The tenant_id is included in JWT claims for request-level validation.
 */
@Entity
@Table(name = "business_rules", indexes = {
    @Index(name = "idx_business_rules_tenant_rule_number", columnList = "tenant_id,rule_number", unique = true),
    @Index(name = "idx_business_rules_tenant_control_point", columnList = "tenant_id,control_point"),
    @Index(name = "idx_business_rules_tenant_active", columnList = "tenant_id,activate_flag")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "rule_number", nullable = false)
    private Integer ruleNumber;

    @Column(name = "control_point", nullable = false, length = 100)
    private String controlPoint;

    @Column(name = "applicability", nullable = false, length = 1)
    private String applicability; // Y or N

    @Column(name = "rule_value", length = 500)
    private String ruleValue;

    @Column(name = "description", length = 1000)
    private String description;

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

    /**
     * Check if rule is applicable (Y) and active.
     */
    public boolean isApplicableAndActive() {
        return "Y".equalsIgnoreCase(applicability) && Boolean.TRUE.equals(activateFlag);
    }
}

