package com.elina.projects.controller;

import com.elina.projects.dto.ProjectCreateDTO;
import com.elina.projects.dto.ProjectDTO;
import com.elina.projects.dto.ProjectDetailsDTO;
import com.elina.projects.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Project management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Projects are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_PROJECTS_EDIT permission.
 * Read operations require PAGE_PROJECTS_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
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
     * List projects with filtering and pagination.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping
    public ResponseEntity<Page<ProjectDTO>> listProjects(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<ProjectDTO> result = projectService.listProjects(search, activeOnly, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Get project by ID.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProjectDTO result = projectService.getProject(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get project details with aggregated summary.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<ProjectDetailsDTO> getProjectDetails(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProjectDetailsDTO result = projectService.getProjectDetails(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new project.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectCreateDTO dto) {
        ProjectDTO result = projectService.createProject(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing project.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectCreateDTO dto) {
        ProjectDTO result = projectService.updateProject(id, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a project (soft delete).
     * Requires: PAGE_PROJECTS_EDIT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}

