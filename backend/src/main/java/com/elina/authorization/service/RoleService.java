package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Role;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.RoleRepository;
import com.elina.authorization.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for role management with tenant-aware operations.
 * 
 * Tenant enforcement: All role operations automatically filter by tenant_id
 * from TenantContext. Roles are tenant-specific.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    public RoleService(RoleRepository roleRepository, TenantRepository tenantRepository) {
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return roleRepository.findAllActiveByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Role findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return roleRepository.findById(id)
                .filter(role -> role.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    @Transactional
    public Role create(Role role, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        role.setTenant(tenant);
        return roleRepository.save(role);
    }

    @Transactional
    public Role update(Long id, Role roleDetails) {
        Long tenantId = TenantContext.getTenantId();
        Role role = roleRepository.findById(id)
                .filter(r -> r.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Role not found"));

        role.setCode(roleDetails.getCode());
        role.setName(roleDetails.getName());
        role.setDescription(roleDetails.getDescription());
        role.setIsActive(roleDetails.getIsActive());

        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Role role = roleRepository.findById(id)
                .filter(r -> r.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Role not found"));
        roleRepository.delete(role);
    }
}

