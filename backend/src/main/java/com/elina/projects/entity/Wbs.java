package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WBS (Work Breakdown Structure) entity with hierarchical support.
 * 
 * Tenant enforcement: WBS belongs to a specific tenant and project.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * 
 * Business rules:
 * - WBS end_date cannot be before start_date (Rule 202)
 * - WBS start_date cannot be before parent WBS start_date
 * - WBS end_date cannot be after parent WBS end_date
 * - Confirmed WBS cannot be modified (Rule 301)
 */
@Entity
@Table(name = "wbs", indexes = {
    @Index(name = "idx_wbs_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_wbs_project_id", columnList = "tenant_id,project_id"),
    @Index(name = "idx_wbs_parent_id", columnList = "tenant_id,parent_wbs_id"),
    @Index(name = "idx_wbs_tenant_code", columnList = "tenant_id,wbs_code", unique = true),
    @Index(name = "idx_wbs_tenant_active", columnList = "tenant_id,activate_flag")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wbs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wbs_id")
    private Long wbsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_wbs_id")
    private Wbs parentWbs;

    @OneToMany(mappedBy = "parentWbs", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Wbs> children = new ArrayList<>();

    @Column(name = "wbs_code", nullable = false, length = 50)
    private String wbsCode;

    @Column(name = "wbs_name", nullable = false, length = 200)
    private String wbsName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "level", nullable = false)
    private Integer level = 1; // Hierarchy level (1 = root, 2 = child, etc.)

    @Column(name = "work_center", length = 50)
    private String workCenter; // For authorization filtering

    @Column(name = "cost_center", length = 50)
    private String costCenter; // For authorization filtering

    @Column(name = "planned_qty", precision = 18, scale = 2)
    private BigDecimal plannedQty;

    @Column(name = "actual_qty", precision = 18, scale = 2)
    private BigDecimal actualQty = BigDecimal.ZERO;

    @Column(name = "status", length = 50)
    private String status; // PLANNED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "is_confirmed")
    private Boolean isConfirmed = false;

    @Column(name = "confirmed_on")
    private LocalDateTime confirmedOn;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "lock_date")
    private LocalDate lockDate;

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
        // Calculate level based on parent
        if (parentWbs != null) {
            level = parentWbs.getLevel() + 1;
        } else {
            level = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
        // Recalculate level if parent changed
        if (parentWbs != null) {
            level = parentWbs.getLevel() + 1;
        } else {
            level = 1;
        }
    }
}

