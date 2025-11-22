package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.resource.EquipmentAllocationEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for equipment allocations with tenant-aware filters.
 */
@Repository
public interface EquipmentAllocationRepository extends TenantAwareRepository<EquipmentAllocationEntity, Long> {

    @Query("SELECT ea FROM EquipmentAllocationEntity ea WHERE ea.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ea.wbs.wbsId = :wbsId ORDER BY ea.startDate")
    List<EquipmentAllocationEntity> findByWbsId(@Param("wbsId") Long wbsId);

    @Query("SELECT ea FROM EquipmentAllocationEntity ea WHERE ea.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ea.wbs.wbsId = :wbsId AND ea.startDate <= :endDate AND ea.endDate >= :startDate ORDER BY ea.startDate")
    List<EquipmentAllocationEntity> findByWbsIdAndDateRange(@Param("wbsId") Long wbsId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END FROM EquipmentAllocationEntity ea " +
           "WHERE ea.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ea.wbs.wbsId = :wbsId " +
           "AND ea.equipment.equipmentId = :equipmentId " +
           "AND (:excludeId IS NULL OR ea.allocationId <> :excludeId) " +
           "AND ea.startDate <= :endDate AND ea.endDate >= :startDate")
    boolean existsOverlappingAllocation(@Param("wbsId") Long wbsId,
                                        @Param("equipmentId") Long equipmentId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(ea.totalCost), 0) FROM EquipmentAllocationEntity ea WHERE ea.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND ea.wbs.wbsId = :wbsId")
    java.math.BigDecimal sumCostByWbsId(@Param("wbsId") Long wbsId);
}

