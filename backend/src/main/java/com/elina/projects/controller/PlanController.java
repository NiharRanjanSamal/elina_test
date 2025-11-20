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
     * Revert to a plan version.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}/revert")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<PlanVersionDTO> revertToPlanVersion(@PathVariable Long id) {
        PlanVersionDTO result = planService.revertToPlanVersion(id);
        return ResponseEntity.ok(result);
    }
}

