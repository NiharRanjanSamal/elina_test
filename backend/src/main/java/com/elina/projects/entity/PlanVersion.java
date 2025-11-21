package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plan Version entity representing a versioned plan for a task.
 * 
 * Tenant enforcement: Plan versions belong to a specific tenant and task.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * 
 * Business rules:
 * - Plan version date cannot be in future (Rule 402)
 */
@Entity
@Table(name = "plan_versions", indexes = {
    @Index(name = "idx_plan_versions_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_plan_versions_tenant_task_version", columnList = "tenant_id,task_id,version_no"),
    @Index(name = "idx_plan_versions_tenant_active", columnList = "tenant_id,activate_flag")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_plan_versions_task_version", columnNames = {"task_id", "version_no"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_version_id")
    private Long planVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "version_date", nullable = false)
    private LocalDate versionDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

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

