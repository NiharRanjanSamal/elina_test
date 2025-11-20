package com.elina.projects.controller;

import com.elina.projects.dto.WbsCreateDTO;
import com.elina.projects.dto.WbsDTO;
import com.elina.projects.service.WbsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WBS management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. WBS are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_PROJECTS_EDIT permission.
 * Read operations require PAGE_PROJECTS_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/wbs")
public class WbsController {

    private final WbsService wbsService;

    public WbsController(WbsService wbsService) {
        this.wbsService = wbsService;
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
     * Get WBS hierarchy for a project.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/project/{projectId}/hierarchy")
    public ResponseEntity<List<WbsDTO>> getWbsHierarchy(@PathVariable Long projectId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<WbsDTO> result = wbsService.getWbsHierarchy(projectId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all WBS for a project.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<WbsDTO>> getWbsByProjectId(@PathVariable Long projectId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<WbsDTO> result = wbsService.getWbsByProjectId(projectId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get WBS by ID.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}")
    public ResponseEntity<WbsDTO> getWbs(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        WbsDTO result = wbsService.getWbs(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new WBS.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<WbsDTO> createWbs(@Valid @RequestBody WbsCreateDTO dto) {
        WbsDTO result = wbsService.createWbs(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing WBS.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<WbsDTO> updateWbs(
            @PathVariable Long id,
            @Valid @RequestBody WbsCreateDTO dto) {
        WbsDTO result = wbsService.updateWbs(id, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Move WBS to different parent.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}/move")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<WbsDTO> moveWbs(
            @PathVariable Long id,
            @RequestParam(required = false) Long parentWbsId) {
        WbsDTO result = wbsService.moveWbs(id, parentWbsId);
        return ResponseEntity.ok(result);
    }

    /**
     * Compute WBS planned and confirmed qty.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}/compute-qty")
    public ResponseEntity<WbsDTO> computeWbsPlannedAndConfirmedQty(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        WbsDTO result = wbsService.computeWbsPlannedAndConfirmedQty(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a WBS (soft delete).
     * Requires: PAGE_PROJECTS_EDIT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteWbs(@PathVariable Long id) {
        wbsService.deleteWbs(id);
        return ResponseEntity.noContent().build();
    }
}

