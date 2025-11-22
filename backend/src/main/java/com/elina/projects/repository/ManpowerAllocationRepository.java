package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.resource.ManpowerAllocationEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for manpower allocations with tenant isolation and date-range filtering.
 */
@Repository
public interface ManpowerAllocationRepository extends TenantAwareRepository<ManpowerAllocationEntity, Long> {

    @Query("SELECT ma FROM ManpowerAllocationEntity ma WHERE ma.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ma.wbs.wbsId = :wbsId ORDER BY ma.startDate")
    List<ManpowerAllocationEntity> findByWbsId(@Param("wbsId") Long wbsId);

    @Query("SELECT ma FROM ManpowerAllocationEntity ma WHERE ma.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ma.wbs.wbsId = :wbsId AND ma.startDate <= :endDate AND ma.endDate >= :startDate ORDER BY ma.startDate")
    List<ManpowerAllocationEntity> findByWbsIdAndDateRange(@Param("wbsId") Long wbsId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END FROM ManpowerAllocationEntity ma " +
           "WHERE ma.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ma.wbs.wbsId = :wbsId " +
           "AND ma.employee.employeeId = :employeeId " +
           "AND (:excludeId IS NULL OR ma.allocationId <> :excludeId) " +
           "AND ma.startDate <= :endDate AND ma.endDate >= :startDate")
    boolean existsOverlappingAllocation(@Param("wbsId") Long wbsId,
                                        @Param("employeeId") Long employeeId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(ma.totalCost), 0) FROM ManpowerAllocationEntity ma WHERE ma.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ma.wbs.wbsId = :wbsId")
    java.math.BigDecimal sumCostByWbsId(@Param("wbsId") Long wbsId);
}

