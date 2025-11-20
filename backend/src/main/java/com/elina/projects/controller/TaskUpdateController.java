package com.elina.projects.controller;

import com.elina.projects.dto.*;
import com.elina.projects.service.TaskUpdateService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
     * Get unified day-wise updates for a task (merges plan lines with existing updates).
     * Requires: PAGE_TASK_UPDATE_VIEW or PAGE_PROJECTS_VIEW
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskUpdateDayWiseDTO>> getUpdatesForTask(@PathVariable Long taskId) {
        if (!hasPermission("PAGE_TASK_UPDATE_VIEW") && !hasPermission("PAGE_PROJECTS_VIEW") 
            && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TaskUpdateDayWiseDTO> result = taskUpdateService.getUpdatesForTask(taskId);
        return ResponseEntity.ok(result);
    }

    /**
     * List task updates for a task (legacy endpoint - returns simple DTOs).
     * Requires: PAGE_TASK_UPDATE_VIEW or PAGE_PROJECTS_VIEW
     */
    @GetMapping("/task/{taskId}/list")
    public ResponseEntity<List<TaskUpdateDTO>> getTaskUpdates(@PathVariable Long taskId) {
        if (!hasPermission("PAGE_TASK_UPDATE_VIEW") && !hasPermission("PAGE_PROJECTS_VIEW") 
            && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TaskUpdateDTO> result = taskUpdateService.getTaskUpdates(taskId);
        return ResponseEntity.ok(result);
    }

    /**
     * Save or update multiple day-wise updates in bulk.
     * Requires: PAGE_TASK_UPDATE_EDIT or PAGE_PROJECTS_EDIT
     */
    @PostMapping("/task/{taskId}")
    @PreAuthorize("hasAuthority('PAGE_TASK_UPDATE_EDIT') or hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<List<TaskUpdateDTO>> saveOrUpdateDayWise(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateBulkDTO bulkDTO) {
        // Ensure taskId in path matches DTO
        bulkDTO.setTaskId(taskId);
        List<TaskUpdateDTO> result = taskUpdateService.saveOrUpdateDayWise(bulkDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Create or update a single day-wise task update (legacy endpoint).
     * Requires: PAGE_TASK_UPDATE_EDIT or PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_TASK_UPDATE_EDIT') or hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<TaskUpdateDTO> createOrUpdateTaskUpdate(@Valid @RequestBody TaskUpdateCreateDTO dto) {
        TaskUpdateDTO result = taskUpdateService.createOrUpdateTaskUpdate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Delete a task update.
     * Requires: PAGE_TASK_UPDATE_EDIT or PAGE_PROJECTS_EDIT
     */
    @DeleteMapping("/{updateId}")
    @PreAuthorize("hasAuthority('PAGE_TASK_UPDATE_EDIT') or hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteTaskUpdate(@PathVariable Long updateId) {
        taskUpdateService.deleteTaskUpdate(updateId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get daily summary (plan_qty, actual_qty, variance) for a task within date range.
     * Requires: PAGE_TASK_UPDATE_VIEW or PAGE_PROJECTS_VIEW
     */
    @GetMapping("/task/{taskId}/summary")
    public ResponseEntity<List<TaskUpdateSummaryDTO>> getDailySummary(
            @PathVariable Long taskId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (!hasPermission("PAGE_TASK_UPDATE_VIEW") && !hasPermission("PAGE_PROJECTS_VIEW") 
            && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TaskUpdateSummaryDTO> result = taskUpdateService.getDailySummary(taskId, from, to);
        return ResponseEntity.ok(result);
    }
}

