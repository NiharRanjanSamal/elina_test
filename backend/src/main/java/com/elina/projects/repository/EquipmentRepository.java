package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.resource.EquipmentEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant-aware repository for {@link EquipmentEntity}.
 */
@Repository
public interface EquipmentRepository extends TenantAwareRepository<EquipmentEntity, Long> {

    @Query("SELECT e FROM EquipmentEntity e WHERE e.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:activeOnly = false OR e.activateFlag = true) ORDER BY e.equipmentName")
    List<EquipmentEntity> findAllForTenant(@Param("activeOnly") boolean activeOnly);

    @Query("SELECT e FROM EquipmentEntity e WHERE e.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND e.equipmentId = :equipmentId")
    Optional<EquipmentEntity> findByIdForTenant(@Param("equipmentId") Long equipmentId);

    @Query("SELECT e FROM EquipmentEntity e WHERE e.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:query IS NULL OR LOWER(e.equipmentName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:activeOnly = false OR e.activateFlag = true) ORDER BY e.equipmentName")
    List<EquipmentEntity> search(@Param("query") String query, @Param("activeOnly") boolean activeOnly);
}

