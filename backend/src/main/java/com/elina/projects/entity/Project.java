package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Project entity representing a project in the system.
 * 
 * Tenant enforcement: Projects belong to a specific tenant.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * The tenant_id is included in JWT claims for request-level validation.
 * 
 * Business rules:
 * - Project start_date cannot be in future (Rule 201)
 * - Project end_date must be after start_date (Rule 202)
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_projects_tenant_code", columnList = "tenant_id,project_code", unique = true),
    @Index(name = "idx_projects_tenant_active", columnList = "tenant_id,activate_flag")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "project_code", nullable = false, length = 50)
    private String projectCode;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 50)
    private String status; // ACTIVE, COMPLETED, ON_HOLD, CANCELLED

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

