package com.elina.projects.controller;

import com.elina.projects.dto.*;
import com.elina.projects.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plan Version management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Plan versions are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_PROJECTS_EDIT permission.
 * Read operations require PAGE_PROJECTS_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /**
     * Check if user has required permission.
     */
    private boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission) || 
                             a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
    }

    /**
     * List plan versions for a task.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<PlanVersionDTO>> listPlanVersions(@PathVariable Long taskId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PlanVersionDTO> result = planService.listPlanVersions(taskId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get plan version by ID.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanVersionDTO> getPlanVersion(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PlanVersionDTO result = planService.getPlanVersion(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get plan lines for a version.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}/lines")
    public ResponseEntity<List<PlanLineDTO>> getPlanLines(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PlanLineDTO> result = planService.getPlanLines(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new plan version with lines.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<PlanVersionDTO> createPlanVersion(@Valid @RequestBody PlanVersionCreateDTO dto) {
        PlanVersionDTO result = planService.createPlanVersion(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Set a plan version as active.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<PlanVersionDTO> setActiveVersion(@PathVariable Long id) {
        PlanVersionDTO result = planService.setActiveVersion(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get plan version details with lines.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<PlanVersionDTO> getPlanVersionDetails(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PlanVersionDTO result = planService.getPlanVersionDetails(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create or update plan lines for a version.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<PlanVersionDTO> createOrUpdatePlanLines(
            @PathVariable Long id,
            @Valid @RequestBody List<PlanLineCreateDTO> lines) {
        PlanVersionDTO result = planService.createOrUpdatePlanLines(id, lines, null);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a plan version.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deletePlanVersion(@PathVariable Long id) {
        planService.deletePlanVersion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Revert to a plan version.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}/revert")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<PlanVersionDTO> revertToPlanVersion(@PathVariable Long id) {
        PlanVersionDTO result = planService.revertToPlanVersion(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create plan version using one of three modes: DAILY_ENTRY, DATE_RANGE_SPLIT, or SINGLE_LINE_QUICK.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping("/create-with-mode")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<PlanVersionDTO> createPlanVersionWithMode(
            @Valid @RequestBody PlanCreationModeDTO dto) {
        PlanVersionDTO result = planService.createPlanVersionWithMode(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Compare two plan versions.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/compare/{versionId1}/{versionId2}")
    public ResponseEntity<PlanVersionComparisonDTO> comparePlanVersions(
            @PathVariable Long versionId1,
            @PathVariable Long versionId2) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PlanVersionComparisonDTO result = planService.comparePlanVersions(versionId1, versionId2);
        return ResponseEntity.ok(result);
    }
}

