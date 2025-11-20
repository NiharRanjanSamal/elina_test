package com.elina.projects.controller;

import com.elina.projects.dto.ConfirmationCreateDTO;
import com.elina.projects.dto.ConfirmationDTO;
import com.elina.projects.service.ConfirmationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Confirmation management controller with tenant-aware operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Confirmations are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_PROJECTS_EDIT permission.
 * Read operations require PAGE_PROJECTS_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/confirmations")
public class ConfirmationController {

    private final ConfirmationService confirmationService;

    public ConfirmationController(ConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    /**
     * Confirm a WBS or Task.
     * Requires: PAGE_PROJECTS_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_PROJECTS_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<ConfirmationDTO> confirmEntity(@Valid @RequestBody ConfirmationCreateDTO dto) {
        ConfirmationDTO result = confirmationService.confirmEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

