package com.elina.authorization.controller;

import com.elina.authorization.entity.PageAuthorization;
import com.elina.authorization.service.PageAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Page authorization management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Page authorizations are tenant-specific.
 */
@RestController
@RequestMapping("/api/page-authorizations")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class PageAuthorizationController {

    private final PageAuthorizationService pageAuthorizationService;

    public PageAuthorizationController(PageAuthorizationService pageAuthorizationService) {
        this.pageAuthorizationService = pageAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<List<PageAuthorization>> getAllPageAuthorizations() {
        List<PageAuthorization> pageAuthorizations = pageAuthorizationService.findAll();
        return ResponseEntity.ok(pageAuthorizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageAuthorization> getPageAuthorizationById(@PathVariable Long id) {
        PageAuthorization pageAuth = pageAuthorizationService.findById(id);
        return ResponseEntity.ok(pageAuth);
    }

    @PostMapping
    public ResponseEntity<PageAuthorization> createPageAuthorization(@RequestBody PageAuthorization pageAuth) {
        PageAuthorization createdPageAuth = pageAuthorizationService.create(pageAuth);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPageAuth);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PageAuthorization> updatePageAuthorization(@PathVariable Long id, @RequestBody PageAuthorization pageAuth) {
        PageAuthorization updatedPageAuth = pageAuthorizationService.update(id, pageAuth);
        return ResponseEntity.ok(updatedPageAuth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePageAuthorization(@PathVariable Long id) {
        pageAuthorizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

