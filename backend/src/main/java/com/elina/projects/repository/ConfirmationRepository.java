package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.Confirmation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Confirmation entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 */
@Repository
public interface ConfirmationRepository extends TenantAwareRepository<Confirmation, Long> {
    
    /**
     * Find confirmation by entity type and entity ID.
     */
    @Query("SELECT c FROM Confirmation c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType = :entityType AND c.entityId = :entityId")
    Optional<Confirmation> findByEntityTypeAndEntityId(
        @Param("entityType") String entityType,
        @Param("entityId") Long entityId
    );

    /**
     * Find all confirmations for a project.
     */
    @Query("SELECT c FROM Confirmation c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType IN ('WBS', 'TASK') " +
           "ORDER BY c.confirmationDate DESC, c.confirmedOn DESC")
    List<Confirmation> findAllForProject();

    /**
     * Check if entity is confirmed.
     */
    @Query("SELECT COUNT(c) > 0 FROM Confirmation c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType = :entityType AND c.entityId = :entityId")
    boolean existsByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    /**
     * Find confirmation by entity type and entity ID that covers a specific date.
     * Used to check if an update date is locked by a confirmation.
     * A confirmation "covers" a date if confirmation_date >= update_date.
     */
    @Query("SELECT c FROM Confirmation c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType = :entityType AND c.entityId = :entityId " +
           "AND c.confirmationDate >= :updateDate")
    Optional<Confirmation> findLockingConfirmation(
        @Param("entityType") String entityType,
        @Param("entityId") Long entityId,
        @Param("updateDate") java.time.LocalDate updateDate
    );
}

