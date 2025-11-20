package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Project entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 * This ensures projects from one tenant cannot access projects from another tenant.
 */
@Repository
public interface ProjectRepository extends TenantAwareRepository<Project, Long> {
    
    /**
     * Find project by tenant and project code.
     * Uses SpEL to retrieve tenantId from TenantContext.
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND p.projectCode = :projectCode")
    Optional<Project> findByProjectCode(@Param("projectCode") String projectCode);

    /**
     * Find all active projects for tenant with pagination and filtering.
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:activeOnly IS NULL OR :activeOnly = false OR p.activateFlag = true) " +
           "AND (:search IS NULL OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.projectCode")
    Page<Project> findWithFilters(
        @Param("activeOnly") Boolean activeOnly,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Find all active projects for tenant.
     */
    @Query("SELECT p FROM Project p WHERE p.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND p.activateFlag = true ORDER BY p.projectCode")
    List<Project> findAllActive();

    /**
     * Check if project code exists for tenant.
     */
    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} AND p.projectCode = :projectCode")
    boolean existsByProjectCode(@Param("projectCode") String projectCode);
}

