package com.elina.authorization.controller;

import com.elina.authorization.dto.BulkUploadResult;
import com.elina.authorization.dto.MasterCodeCountDTO;
import com.elina.authorization.dto.MasterCodeCreateDTO;
import com.elina.authorization.dto.MasterCodeDTO;
import com.elina.authorization.service.MasterCodeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Master Code management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Master codes are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_MASTER_DATA_EDIT permission.
 * Read operations require PAGE_MASTER_DATA_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/master-codes")
public class MasterCodeController {

    private final MasterCodeService masterCodeService;

    public MasterCodeController(MasterCodeService masterCodeService) {
        this.masterCodeService = masterCodeService;
    }

    /**
     * Check if user has required permission.
     * Permissions are now included as authorities in TenantFilter.
     */
    private boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        
        // Check if user has the permission in authorities (includes both roles and permissions)
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission) || 
                             a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
    }

    /**
     * List master codes with filtering and pagination.
     * Requires: PAGE_MASTER_DATA_VIEW or higher
     */
    @GetMapping
    public ResponseEntity<Page<MasterCodeDTO>> listMasterCodes(
            @RequestParam(required = false) String codeType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        if (!hasPermission("PAGE_MASTER_DATA_VIEW") && !hasPermission("PAGE_MASTER_DATA_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<MasterCodeDTO> result = masterCodeService.listMasterCodes(codeType, search, activeOnly, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Get master code by ID.
     * Requires: PAGE_MASTER_DATA_VIEW or higher
     */
    @GetMapping("/{id}")
    public ResponseEntity<MasterCodeDTO> getMasterCode(@PathVariable Long id) {
        if (!hasPermission("PAGE_MASTER_DATA_VIEW") && !hasPermission("PAGE_MASTER_DATA_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        MasterCodeDTO result = masterCodeService.getMasterCode(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get active master codes by code type (cached).
     * Requires: PAGE_MASTER_DATA_VIEW or higher
     */
    @GetMapping("/by-type/{codeType}")
    public ResponseEntity<List<MasterCodeDTO>> getActiveMasterCodesByType(@PathVariable String codeType) {
        if (!hasPermission("PAGE_MASTER_DATA_VIEW") && !hasPermission("PAGE_MASTER_DATA_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<MasterCodeDTO> result = masterCodeService.getActiveMasterCodesByType(codeType);
        return ResponseEntity.ok(result);
    }

    /**
     * Get count of active codes by type (for UI decision: radio vs dropdown).
     * Requires: PAGE_MASTER_DATA_VIEW or higher
     */
    @GetMapping("/count")
    public ResponseEntity<MasterCodeCountDTO> getActiveCount(
            @RequestParam String codeType,
            @RequestParam(required = false, defaultValue = "3") int limit) {
        
        if (!hasPermission("PAGE_MASTER_DATA_VIEW") && !hasPermission("PAGE_MASTER_DATA_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        MasterCodeCountDTO result = masterCodeService.getActiveCountByType(codeType, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all distinct code types for the tenant.
     * Requires: PAGE_MASTER_DATA_VIEW or higher
     */
    @GetMapping("/code-types")
    public ResponseEntity<List<String>> getAllCodeTypes() {
        if (!hasPermission("PAGE_MASTER_DATA_VIEW") && !hasPermission("PAGE_MASTER_DATA_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<String> result = masterCodeService.getAllCodeTypes();
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new master code.
     * Requires: PAGE_MASTER_DATA_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_MASTER_DATA_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<MasterCodeDTO> createMasterCode(@Valid @RequestBody MasterCodeCreateDTO dto) {
        MasterCodeDTO result = masterCodeService.createMasterCode(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing master code.
     * Requires: PAGE_MASTER_DATA_EDIT
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_MASTER_DATA_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<MasterCodeDTO> updateMasterCode(
            @PathVariable Long id,
            @Valid @RequestBody MasterCodeCreateDTO dto) {
        MasterCodeDTO result = masterCodeService.updateMasterCode(id, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a master code.
     * Requires: PAGE_MASTER_DATA_EDIT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_MASTER_DATA_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteMasterCode(@PathVariable Long id) {
        masterCodeService.deleteMasterCode(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk upload master codes from CSV or Excel file.
     * Requires: PAGE_MASTER_DATA_EDIT
     */
    @PostMapping("/bulk-upload")
    @PreAuthorize("hasAuthority('PAGE_MASTER_DATA_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<BulkUploadResult> bulkUploadMasterCodes(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "true") boolean dryRun) {
        
        BulkUploadResult result = masterCodeService.bulkUploadMasterCodes(file, dryRun);
        return ResponseEntity.ok(result);
    }

    /**
     * Refresh cache for master codes (admin only).
     * Requires: ROLE_SYSTEM_ADMIN
     */
    @PostMapping("/refresh-cache")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> refreshCache(@RequestParam(required = false) String codeType) {
        masterCodeService.refreshCache(codeType);
        return ResponseEntity.ok(Map.of("message", "Cache refreshed successfully"));
    }
}

