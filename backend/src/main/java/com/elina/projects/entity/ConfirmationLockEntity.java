package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents the latest frozen date for a WBS.
 * Only one record exists per WBS to simplify lock lookups.
 */
@Entity
@Table(
    name = "confirmation_locks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_confirmation_locks_wbs",
            columnNames = {"tenant_id", "wbs_id"}
        )
    },
    indexes = {
        @Index(name = "idx_confirmation_locks_tenant", columnList = "tenant_id"),
        @Index(name = "idx_confirmation_locks_wbs", columnList = "tenant_id,wbs_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ConfirmationLockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lock_id")
    private Long lockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wbs_id", nullable = false)
    private Wbs wbs;

    @Column(name = "lock_date", nullable = false)
    private LocalDate lockDate;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
        if (updatedOn == null) {
            updatedOn = createdOn;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
    }
}


