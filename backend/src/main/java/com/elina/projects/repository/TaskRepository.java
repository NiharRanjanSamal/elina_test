package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Task entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * This ensures tasks from one tenant cannot access tasks from another tenant.
 */
@Repository
public interface TaskRepository extends TenantAwareRepository<Task, Long> {
    
    /**
     * Find task by tenant and task code.
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND t.taskCode = :taskCode")
    Optional<Task> findByTaskCode(@Param("taskCode") String taskCode);

    /**
     * Find all tasks for a WBS.
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND t.wbs.wbsId = :wbsId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR t.activateFlag = true) " +
           "ORDER BY t.taskCode")
    List<Task> findByWbsId(@Param("wbsId") Long wbsId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find all tasks for a project.
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND t.project.projectId = :projectId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR t.activateFlag = true) " +
           "ORDER BY t.taskCode")
    List<Task> findByProjectId(@Param("projectId") Long projectId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find tasks with pagination and filtering.
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:projectId IS NULL OR t.project.projectId = :projectId) " +
           "AND (:wbsId IS NULL OR t.wbs.wbsId = :wbsId) " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR t.activateFlag = true) " +
           "AND (:search IS NULL OR LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.taskName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.taskCode")
    Page<Task> findWithFilters(
        @Param("projectId") Long projectId,
        @Param("wbsId") Long wbsId,
        @Param("activeOnly") Boolean activeOnly,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Check if task code exists for tenant.
     */
    @Query("SELECT COUNT(t) > 0 FROM Task t WHERE t.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND t.taskCode = :taskCode")
    boolean existsByTaskCode(@Param("taskCode") String taskCode);
}

