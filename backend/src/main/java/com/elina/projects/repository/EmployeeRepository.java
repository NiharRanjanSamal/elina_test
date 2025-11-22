package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.resource.EmployeeEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant-aware repository for {@link EmployeeEntity}.
 */
@Repository
public interface EmployeeRepository extends TenantAwareRepository<EmployeeEntity, Long> {

    /**
     * Find active employees for the current tenant.
     */
    @Query("SELECT e FROM EmployeeEntity e WHERE e.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:activeOnly = false OR e.activateFlag = true) ORDER BY e.name")
    List<EmployeeEntity> findAllForTenant(@Param("activeOnly") boolean activeOnly);

    /**
     * Find employee by ID with tenant isolation.
     */
    @Query("SELECT e FROM EmployeeEntity e WHERE e.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND e.employeeId = :employeeId")
    Optional<EmployeeEntity> findByIdForTenant(@Param("employeeId") Long employeeId);

    /**
     * Search employees by name for dropdown usage.
     */
    @Query("SELECT e FROM EmployeeEntity e WHERE e.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:query IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:activeOnly = false OR e.activateFlag = true) ORDER BY e.name")
    List<EmployeeEntity> search(@Param("query") String query, @Param("activeOnly") boolean activeOnly);
}

