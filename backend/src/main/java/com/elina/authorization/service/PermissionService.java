package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Permission;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.PermissionRepository;
import com.elina.authorization.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for permission management with tenant-aware operations.
 * 
 * Tenant enforcement: All permission operations automatically filter by tenant_id
 * from TenantContext. Permissions are tenant-specific.
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;

    public PermissionService(PermissionRepository permissionRepository, TenantRepository tenantRepository) {
        this.permissionRepository = permissionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return permissionRepository.findAllActiveByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Permission findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return permissionRepository.findById(id)
                .filter(permission -> permission.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Permission not found"));
    }

    @Transactional
    public Permission create(Permission permission, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        permission.setTenant(tenant);
        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission update(Long id, Permission permissionDetails) {
        Long tenantId = TenantContext.getTenantId();
        Permission permission = permissionRepository.findById(id)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        permission.setCode(permissionDetails.getCode());
        permission.setName(permissionDetails.getName());
        permission.setDescription(permissionDetails.getDescription());
        permission.setResourceType(permissionDetails.getResourceType());
        permission.setAction(permissionDetails.getAction());
        permission.setIsActive(permissionDetails.getIsActive());

        return permissionRepository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Permission permission = permissionRepository.findById(id)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        permissionRepository.delete(permission);
    }
}

