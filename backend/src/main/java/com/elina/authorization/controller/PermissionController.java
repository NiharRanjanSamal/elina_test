package com.elina.authorization.controller;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Permission;
import com.elina.authorization.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Permission management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Permissions are tenant-specific.
 */
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.findAll();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long id) {
        Permission permission = permissionService.findById(id);
        return ResponseEntity.ok(permission);
    }

    @PostMapping
    public ResponseEntity<Permission> createPermission(@RequestBody Permission permission) {
        Long tenantId = TenantContext.getTenantId();
        Permission createdPermission = permissionService.create(permission, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPermission);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        Permission updatedPermission = permissionService.update(id, permission);
        return ResponseEntity.ok(updatedPermission);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

