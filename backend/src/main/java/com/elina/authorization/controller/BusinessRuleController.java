package com.elina.authorization.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elina.authorization.dto.BusinessRuleCreateDTO;
import com.elina.authorization.dto.BusinessRuleDTO;
import com.elina.authorization.service.BusinessRuleService;

import jakarta.validation.Valid;

/**
 * Business Rule management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. Business rules are tenant-specific.
 * 
 * Authorization: Write operations require PAGE_BUSINESS_RULES_EDIT permission.
 * Read operations require PAGE_BUSINESS_RULES_VIEW or higher permission.
 */
@RestController
@RequestMapping("/api/business-rules")
public class BusinessRuleController {

    private final BusinessRuleService businessRuleService;

    public BusinessRuleController(BusinessRuleService businessRuleService) {
        this.businessRuleService = businessRuleService;
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
     * List all business rules for the tenant.
     * Requires: PAGE_BUSINESS_RULES_VIEW or higher
     */
    @GetMapping
    public ResponseEntity<List<BusinessRuleDTO>> listBusinessRules() {
        if (!hasPermission("PAGE_BUSINESS_RULES_VIEW") && !hasPermission("PAGE_BUSINESS_RULES_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<BusinessRuleDTO> result = businessRuleService.listBusinessRules();
        return ResponseEntity.ok(result);
    }

    /**
     * Get business rule by ID.
     * Requires: PAGE_BUSINESS_RULES_VIEW or higher
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessRuleDTO> getBusinessRule(@PathVariable Long id) {
        if (!hasPermission("PAGE_BUSINESS_RULES_VIEW") && !hasPermission("PAGE_BUSINESS_RULES_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BusinessRuleDTO result = businessRuleService.getBusinessRule(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get business rule by rule number.
     * Requires: PAGE_BUSINESS_RULES_VIEW or higher
     */
    @GetMapping("/by-number/{ruleNumber}")
    public ResponseEntity<BusinessRuleDTO> getBusinessRuleByNumber(@PathVariable Integer ruleNumber) {
        if (!hasPermission("PAGE_BUSINESS_RULES_VIEW") && !hasPermission("PAGE_BUSINESS_RULES_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BusinessRuleDTO result = businessRuleService.getBusinessRuleByNumber(ruleNumber);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all control points.
     * Requires: PAGE_BUSINESS_RULES_VIEW or higher
     */
    @GetMapping("/control-points")
    public ResponseEntity<List<String>> getAllControlPoints() {
        if (!hasPermission("PAGE_BUSINESS_RULES_VIEW") && !hasPermission("PAGE_BUSINESS_RULES_EDIT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<String> result = businessRuleService.getAllControlPoints();
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new business rule.
     * Requires: PAGE_BUSINESS_RULES_EDIT
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PAGE_BUSINESS_RULES_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<BusinessRuleDTO> createBusinessRule(@Valid @RequestBody BusinessRuleCreateDTO dto) {
        BusinessRuleDTO result = businessRuleService.createBusinessRule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing business rule.
     * Requires: PAGE_BUSINESS_RULES_EDIT
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_BUSINESS_RULES_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<BusinessRuleDTO> updateBusinessRule(
            @PathVariable Long id,
            @Valid @RequestBody BusinessRuleCreateDTO dto) {
        BusinessRuleDTO result = businessRuleService.updateBusinessRule(id, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a business rule.
     * Requires: PAGE_BUSINESS_RULES_EDIT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAGE_BUSINESS_RULES_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteBusinessRule(@PathVariable Long id) {
        businessRuleService.deleteBusinessRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle activate flag for a business rule.
     * Requires: PAGE_BUSINESS_RULES_EDIT
     */
    @PutMapping("/{id}/activate-toggle")
    @PreAuthorize("hasAuthority('PAGE_BUSINESS_RULES_EDIT') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<BusinessRuleDTO> toggleActivateFlag(@PathVariable Long id) {
        BusinessRuleDTO result = businessRuleService.toggleActivateFlag(id);
        return ResponseEntity.ok(result);
    }
}

