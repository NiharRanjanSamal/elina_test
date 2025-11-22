package com.elina.projects.controller;

import com.elina.projects.dto.resource.*;
import com.elina.projects.service.ResourceAllocationService;
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
 * REST controller for manpower and equipment allocation management.
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceAllocationController {

    private final ResourceAllocationService resourceAllocationService;

    public ResourceAllocationController(ResourceAllocationService resourceAllocationService) {
        this.resourceAllocationService = resourceAllocationService;
    }

    private boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission) || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
    }

    @GetMapping("/manpower/wbs/{wbsId}")
    public ResponseEntity<List<ManpowerAllocationResponseDTO>> getManpowerAllocations(@PathVariable Long wbsId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.getManpowerAllocationsForWbs(wbsId));
    }

    @GetMapping("/equipment/wbs/{wbsId}")
    public ResponseEntity<List<EquipmentAllocationResponseDTO>> getEquipmentAllocations(@PathVariable Long wbsId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.getEquipmentAllocationsForWbs(wbsId));
    }

    @PostMapping("/manpower")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ManpowerAllocationResponseDTO> allocateManpower(@Valid @RequestBody ManpowerAllocationRequestDTO dto) {
        ManpowerAllocationResponseDTO response = resourceAllocationService.allocateEmployeeToWbs(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/manpower/{allocationId}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ManpowerAllocationResponseDTO> updateManpower(@PathVariable Long allocationId,
                                                                        @Valid @RequestBody ManpowerAllocationRequestDTO dto) {
        return ResponseEntity.ok(resourceAllocationService.updateManpowerAllocation(allocationId, dto));
    }

    @DeleteMapping("/manpower/{allocationId}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteManpower(@PathVariable Long allocationId) {
        resourceAllocationService.deleteManpowerAllocation(allocationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/equipment")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<EquipmentAllocationResponseDTO> allocateEquipment(@Valid @RequestBody EquipmentAllocationRequestDTO dto) {
        EquipmentAllocationResponseDTO response = resourceAllocationService.allocateEquipmentToWbs(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/equipment/{allocationId}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<EquipmentAllocationResponseDTO> updateEquipment(@PathVariable Long allocationId,
                                                                          @Valid @RequestBody EquipmentAllocationRequestDTO dto) {
        return ResponseEntity.ok(resourceAllocationService.updateEquipmentAllocation(allocationId, dto));
    }

    @DeleteMapping("/equipment/{allocationId}")
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long allocationId) {
        resourceAllocationService.deleteEquipmentAllocation(allocationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/manpower/options")
    public ResponseEntity<List<ResourceOptionDTO>> getManpowerOptions(@RequestParam(required = false) String search) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.getActiveEmployees(search));
    }

    @GetMapping("/equipment/options")
    public ResponseEntity<List<ResourceOptionDTO>> getEquipmentOptions(@RequestParam(required = false) String search) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.getActiveEquipment(search));
    }

    @GetMapping("/timeline/wbs/{wbsId}")
    public ResponseEntity<List<AllocationTimelineItemDTO>> getTimeline(@PathVariable Long wbsId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.getTimeline(wbsId));
    }

    @GetMapping("/cost/wbs/{wbsId}")
    public ResponseEntity<ResourceCostSummaryDTO> getCostSummary(@PathVariable Long wbsId) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.getCostSummaryForWbs(wbsId));
    }

    @GetMapping("/cost/manpower/{employeeId}")
    public ResponseEntity<ResourceCostBreakdown> previewManpowerCost(@PathVariable Long employeeId,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.previewEmployeeCost(employeeId, startDate, endDate));
    }

    @GetMapping("/cost/equipment/{equipmentId}")
    public ResponseEntity<ResourceCostBreakdown> previewEquipmentCost(@PathVariable Long equipmentId,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!hasPermission("PAGE_PROJECTS_VIEW") && !hasPermission("PAGE_PROJECTS_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(resourceAllocationService.previewEquipmentCost(equipmentId, startDate, endDate));
    }
}

