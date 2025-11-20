package com.elina.projects.controller;

import com.elina.projects.dto.TaskUpdateCreateDTO;
import com.elina.projects.dto.TaskUpdateDTO;
import com.elina.projects.service.TaskUpdateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task Update management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Task updates are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_PROJECTS_EDIT permission.
 * Read operations require PAGE_PROJECTS_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/task-updates")
public class TaskUpdateController {

    private final TaskUpdateService taskUpdateService;

    public TaskUpdateController(TaskUpdateService taskUpdateService) {
        this.taskUpdateService = taskUpdateService;
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
     * List task updates for a task.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskUpdateDTO>> getTaskUpdates(@PathVariable Long taskId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TaskUpdateDTO> result = taskUpdateService.getTaskUpdates(taskId);
        return ResponseEntity.ok(result);
    }

    /**
     * Create or update a day-wise task update.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<TaskUpdateDTO> createOrUpdateTaskUpdate(@Valid @RequestBody TaskUpdateCreateDTO dto) {
        TaskUpdateDTO result = taskUpdateService.createOrUpdateTaskUpdate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

