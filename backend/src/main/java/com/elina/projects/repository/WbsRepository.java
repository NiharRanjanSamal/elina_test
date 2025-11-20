package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.Wbs;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WBS entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * This ensures WBS from one tenant cannot access WBS from another tenant.
 */
@Repository
public interface WbsRepository extends TenantAwareRepository<Wbs, Long> {
    
    /**
     * Find WBS by tenant and WBS code.
     */
    @Query("SELECT w FROM Wbs w WHERE w.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND w.wbsCode = :wbsCode")
    Optional<Wbs> findByWbsCode(@Param("wbsCode") String wbsCode);

    /**
     * Find all WBS for a project (including hierarchy).
     */
    @Query("SELECT w FROM Wbs w WHERE w.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND w.project.projectId = :projectId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR w.activateFlag = true) " +
           "ORDER BY w.level, w.wbsCode")
    List<Wbs> findByProjectId(@Param("projectId") Long projectId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find root WBS (no parent) for a project.
     */
    @Query("SELECT w FROM Wbs w WHERE w.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND w.project.projectId = :projectId AND w.parentWbs IS NULL " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR w.activateFlag = true) " +
           "ORDER BY w.wbsCode")
    List<Wbs> findRootWbsByProjectId(@Param("projectId") Long projectId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Find child WBS for a parent WBS.
     */
    @Query("SELECT w FROM Wbs w WHERE w.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND w.parentWbs.wbsId = :parentWbsId " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR w.activateFlag = true) " +
           "ORDER BY w.wbsCode")
    List<Wbs> findByParentWbsId(@Param("parentWbsId") Long parentWbsId, @Param("activeOnly") Boolean activeOnly);

    /**
     * Check if WBS code exists for tenant.
     */
    @Query("SELECT COUNT(w) > 0 FROM Wbs w WHERE w.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND w.wbsCode = :wbsCode")
    boolean existsByWbsCode(@Param("wbsCode") String wbsCode);

    /**
     * Find WBS by work center and cost center for authorization filtering.
     */
    @Query("SELECT w FROM Wbs w WHERE w.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:workCenter IS NULL OR w.workCenter = :workCenter) " +
           "AND (:costCenter IS NULL OR w.costCenter = :costCenter) " +
           "AND w.activateFlag = true")
    List<Wbs> findByWorkCenterAndCostCenter(@Param("workCenter") String workCenter, @Param("costCenter") String costCenter);
}

