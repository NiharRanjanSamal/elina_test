package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.PlanVersion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PlanVersion entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 */
@Repository
public interface PlanVersionRepository extends TenantAwareRepository<PlanVersion, Long> {
    
    /**
     * Find all plan versions for a task, ordered by version number descending.
     */
    @Query("SELECT pv FROM PlanVersion pv WHERE pv.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pv.task.taskId = :taskId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR pv.activateFlag = true) " +
           "ORDER BY pv.versionNo DESC")
    List<PlanVersion> findByTaskId(@Param("taskId") Long taskId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find current plan version for a task.
     */
    @Query("SELECT pv FROM PlanVersion pv WHERE pv.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pv.task.taskId = :taskId AND pv.isActive = true AND pv.activateFlag = true")
    Optional<PlanVersion> findCurrentByTaskId(@Param("taskId") Long taskId);

    /**
     * Find plan version by task and version number.
     */
    @Query("SELECT pv FROM PlanVersion pv WHERE pv.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pv.task.taskId = :taskId AND pv.versionNo = :versionNo")
    Optional<PlanVersion> findByTaskIdAndVersionNo(@Param("taskId") Long taskId, @Param("versionNo") Integer versionNo);

    /**
     * Find maximum version number for a task.
     */
    @Query("SELECT MAX(pv.versionNo) FROM PlanVersion pv WHERE pv.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pv.task.taskId = :taskId")
    Integer findMaxVersionNoByTaskId(@Param("taskId") Long taskId);
}

