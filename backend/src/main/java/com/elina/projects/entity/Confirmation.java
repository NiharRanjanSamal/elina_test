package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Confirmation entity representing a confirmation record.
 * 
 * Tenant enforcement: Confirmations belong to a specific tenant.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * 
 * Business rules:
 * - Confirmed entries cannot be overwritten (Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN)
 */
@Entity
@Table(name = "confirmations", indexes = {
    @Index(name = "idx_confirmations_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_confirmations_entity", columnList = "tenant_id,entity_type,entity_id"),
    @Index(name = "idx_confirmations_date", columnList = "tenant_id,confirmation_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Confirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confirmation_id")
    private Long confirmationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // WBS, TASK

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "confirmation_date", nullable = false)
    private LocalDate confirmationDate;

    @Column(name = "confirmed_by", nullable = false)
    private Long confirmedBy;

    @Column(name = "confirmed_on", nullable = false, updatable = false)
    private LocalDateTime confirmedOn;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
        if (confirmedOn == null) {
            confirmedOn = LocalDateTime.now();
        }
    }
}

