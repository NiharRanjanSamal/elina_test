package com.elina.projects.controller;

import com.elina.projects.dto.TaskCreateDTO;
import com.elina.projects.dto.TaskDTO;
import com.elina.projects.dto.TaskDetailsDTO;
import com.elina.projects.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Tasks are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_PROJECTS_EDIT permission.
 * Read operations require PAGE_PROJECTS_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
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
     * List tasks with filtering and pagination.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping
    public ResponseEntity<Page<TaskDTO>> listTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long wbsId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<TaskDTO> result = taskService.listTasks(projectId, wbsId, activeOnly, search, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all tasks for a WBS.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/wbs/{wbsId}")
    public ResponseEntity<List<TaskDTO>> getTasksByWbsId(@PathVariable Long wbsId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TaskDTO> result = taskService.getTasksByWbsId(wbsId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get task by ID.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTask(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TaskDTO result = taskService.getTask(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get task details with plan versions and updates.
     * Requires: PAGE_PROJECTS_VIEW or higher
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<TaskDetailsDTO> getTaskDetails(@PathVariable Long id) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TaskDetailsDTO result = taskService.getTaskDetails(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new task.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskCreateDTO dto) {
        TaskDTO result = taskService.createTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing task.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskCreateDTO dto) {
        TaskDTO result = taskService.updateTask(id, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a task (soft delete).
     * Requires: PAGE_PROJECTS_EDIT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}

