package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.TaskUpdate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TaskUpdate entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * 
 * Business rules enforced:
 * - Unique constraint on (tenant_id, task_id, update_date)
 */
@Repository
public interface TaskUpdateRepository extends TenantAwareRepository<TaskUpdate, Long> {
    
    /**
     * Find task update by task and date.
     */
    @Query("SELECT tu FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.taskId = :taskId AND tu.updateDate = :updateDate")
    Optional<TaskUpdate> findByTaskIdAndUpdateDate(@Param("taskId") Long taskId, @Param("updateDate") LocalDate updateDate);

    /**
     * Find all task updates for a task, ordered by date descending.
     */
    @Query("SELECT tu FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.taskId = :taskId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR tu.activateFlag = true) " +
           "ORDER BY tu.updateDate DESC")
    List<TaskUpdate> findByTaskId(@Param("taskId") Long taskId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find task updates within a date range.
     */
    @Query("SELECT tu FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.taskId = :taskId " +
           "AND tu.updateDate >= :startDate AND tu.updateDate <= :endDate " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR tu.activateFlag = true) " +
           "ORDER BY tu.updateDate DESC")
    List<TaskUpdate> findByTaskIdAndDateRange(
        @Param("taskId") Long taskId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("activeOnly") Boolean activeOnly
    );

    /**
     * Calculate total actual qty for a task.
     */
    @Query("SELECT COALESCE(SUM(tu.actualQty), 0) FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.taskId = :taskId AND tu.activateFlag = true")
    java.math.BigDecimal getTotalActualQtyByTaskId(@Param("taskId") Long taskId);

    /**
     * Find all task updates for a task, ordered by update date ascending.
     * Used for day-wise grid display.
     */
    @Query("SELECT tu FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.taskId = :taskId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR tu.activateFlag = true) " +
           "ORDER BY tu.updateDate ASC")
    List<TaskUpdate> findByTaskIdOrderByUpdateDate(@Param("taskId") Long taskId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find all task updates for current tenant.
     * Used for tenant-wide reporting.
     */
    @Query("SELECT tu FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR tu.activateFlag = true) " +
           "ORDER BY tu.updateDate DESC")
    List<TaskUpdate> findForTenant(@Param("activeOnly") Boolean activeOnly);

    @Query("SELECT COALESCE(SUM(tu.actualQty), 0) FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.wbs.wbsId = :wbsId AND tu.updateDate = :targetDate AND tu.activateFlag = true")
    BigDecimal sumActualQtyForWbsAndDate(@Param("wbsId") Long wbsId, @Param("targetDate") LocalDate targetDate);

    @Query("SELECT COALESCE(SUM(tu.actualQty), 0) FROM TaskUpdate tu WHERE tu.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND tu.task.wbs.wbsId = :wbsId AND tu.updateDate <= :targetDate AND tu.activateFlag = true")
    BigDecimal sumActualQtyForWbsUpToDate(@Param("wbsId") Long wbsId, @Param("targetDate") LocalDate targetDate);
}

