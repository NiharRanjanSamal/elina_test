package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.PlanLine;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PlanLine entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 */
@Repository
public interface PlanLineRepository extends TenantAwareRepository<PlanLine, Long> {
    
    /**
     * Find all plan lines for a plan version.
     */
    @Query("SELECT pl FROM PlanLine pl WHERE pl.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pl.planVersion.planVersionId = :planVersionId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR pl.activateFlag = true) " +
           "ORDER BY pl.workDate, pl.lineNumber")
    List<PlanLine> findByPlanVersionId(@Param("planVersionId") Long planVersionId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find all plan lines for a task (across all versions).
     */
    @Query("SELECT pl FROM PlanLine pl WHERE pl.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pl.task.taskId = :taskId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR pl.activateFlag = true) " +
           "ORDER BY pl.workDate, pl.lineNumber")
    List<PlanLine> findByTaskId(@Param("taskId") Long taskId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Delete all plan lines for a plan version.
     */
    @Query("DELETE FROM PlanLine pl WHERE pl.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND pl.planVersion.planVersionId = :planVersionId")
    void deleteByPlanVersionId(@Param("planVersionId") Long planVersionId);
}

