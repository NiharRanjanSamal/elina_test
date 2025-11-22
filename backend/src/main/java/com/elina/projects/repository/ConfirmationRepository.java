package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.ConfirmationEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ConfirmationEntity with tenant aware queries.
 */
@Repository
public interface ConfirmationRepository extends TenantAwareRepository<ConfirmationEntity, Long> {

    @Query("SELECT c FROM ConfirmationEntity c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType = 'WBS' AND c.entityId = :wbsId ORDER BY c.confirmationDate DESC, c.confirmationId DESC")
    List<ConfirmationEntity> findByWbsIdOrderByConfirmationDateDesc(@Param("wbsId") Long wbsId);

    @Query("SELECT c FROM ConfirmationEntity c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.confirmationId = :confirmationId")
    Optional<ConfirmationEntity> findByIdForTenant(@Param("confirmationId") Long confirmationId);

    // Note: confirmedQty doesn't exist in database - this method returns 0 for now
    // TODO: Calculate from task_updates if needed
    @Query("SELECT CAST(0 AS java.math.BigDecimal) FROM ConfirmationEntity c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType = 'WBS' AND c.entityId = :wbsId")
    BigDecimal sumConfirmedQtyByWbs(@Param("wbsId") Long wbsId);

    @Query("SELECT MAX(c.confirmationDate) FROM ConfirmationEntity c WHERE c.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND c.entityType = 'WBS' AND c.entityId = :wbsId")
    LocalDate findLatestConfirmationDateForWbs(@Param("wbsId") Long wbsId);

}

