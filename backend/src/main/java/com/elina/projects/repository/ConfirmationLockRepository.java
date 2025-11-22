package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.ConfirmationLockEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for confirmation lock lookups.
 */
@Repository
public interface ConfirmationLockRepository extends TenantAwareRepository<ConfirmationLockEntity, Long> {

    @Query("SELECT cl FROM ConfirmationLockEntity cl WHERE cl.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND cl.wbs.wbsId = :wbsId")
    Optional<ConfirmationLockEntity> findByWbsId(@Param("wbsId") Long wbsId);

    @Query("SELECT cl.lockDate FROM ConfirmationLockEntity cl WHERE cl.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND cl.wbs.wbsId = :wbsId")
    LocalDate findLockDateByWbsId(@Param("wbsId") Long wbsId);
}


