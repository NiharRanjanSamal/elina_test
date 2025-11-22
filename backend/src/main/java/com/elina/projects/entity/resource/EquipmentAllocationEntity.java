package com.elina.projects.entity.resource;

import com.elina.authorization.entity.Tenant;
import com.elina.projects.entity.Wbs;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Equipment allocation entity mapping equipment usage to WBS tasks.
 */
@Entity
@Table(name = "equipment_allocations", indexes = {
        @Index(name = "idx_equipment_alloc_tenant", columnList = "tenant_id"),
        @Index(name = "idx_equipment_alloc_wbs", columnList = "tenant_id,wbs_id"),
        @Index(name = "idx_equipment_alloc_equipment", columnList = "tenant_id,equipment_id"),
        @Index(name = "idx_equipment_alloc_dates", columnList = "tenant_id,start_date,end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wbs_id", nullable = false)
    private Wbs wbs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentEntity equipment;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "hours_assigned", precision = 9, scale = 2)
    private BigDecimal hoursAssigned;

    @Column(name = "duration_days")
    private Long durationDays;

    @Column(name = "daily_rate", precision = 18, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "total_cost", precision = 18, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "remarks", length = 500)
    private String remarks;

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

