package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.PageAuthorization;
import com.elina.authorization.entity.Role;
import com.elina.authorization.entity.User;
import com.elina.authorization.repository.PageAuthorizationRepository;
import com.elina.authorization.repository.RoleRepository;
import com.elina.authorization.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for page authorization management with tenant-aware operations.
 * 
 * Tenant enforcement: All page authorization operations automatically filter
 * by tenant_id from TenantContext. Page authorizations are tenant-specific.
 */
@Service
public class PageAuthorizationService {

    private final PageAuthorizationRepository pageAuthorizationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public PageAuthorizationService(
            PageAuthorizationRepository pageAuthorizationRepository,
            RoleRepository roleRepository,
            UserRepository userRepository) {
        this.pageAuthorizationRepository = pageAuthorizationRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PageAuthorization> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return pageAuthorizationRepository.findAll().stream()
                .filter(pa -> {
                    if (pa.getRole() != null) {
                        return pa.getRole().getTenant().getId().equals(tenantId);
                    }
                    if (pa.getUser() != null) {
                        return pa.getUser().getTenant().getId().equals(tenantId);
                    }
                    return false;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageAuthorization findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return pageAuthorizationRepository.findById(id)
                .filter(pa -> {
                    if (pa.getRole() != null) {
                        return pa.getRole().getTenant().getId().equals(tenantId);
                    }
                    if (pa.getUser() != null) {
                        return pa.getUser().getTenant().getId().equals(tenantId);
                    }
                    return false;
                })
                .orElseThrow(() -> new RuntimeException("Page authorization not found"));
    }

    @Transactional
    public PageAuthorization create(PageAuthorization pageAuth) {
        Long tenantId = TenantContext.getTenantId();
        
        if (pageAuth.getRole() != null) {
            Role role = roleRepository.findById(pageAuth.getRole().getId())
                    .filter(r -> r.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            pageAuth.setRole(role);
        }
        
        if (pageAuth.getUser() != null) {
            User user = userRepository.findById(pageAuth.getUser().getId())
                    .filter(u -> u.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            pageAuth.setUser(user);
        }
        
        return pageAuthorizationRepository.save(pageAuth);
    }

    @Transactional
    public PageAuthorization update(Long id, PageAuthorization pageAuthDetails) {
        Long tenantId = TenantContext.getTenantId();
        PageAuthorization pageAuth = findById(id);
        
        pageAuth.setPagePath(pageAuthDetails.getPagePath());
        pageAuth.setPageName(pageAuthDetails.getPageName());
        pageAuth.setIsAllowed(pageAuthDetails.getIsAllowed());
        
        if (pageAuthDetails.getRole() != null) {
            Role role = roleRepository.findById(pageAuthDetails.getRole().getId())
                    .filter(r -> r.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            pageAuth.setRole(role);
        }
        
        if (pageAuthDetails.getUser() != null) {
            User user = userRepository.findById(pageAuthDetails.getUser().getId())
                    .filter(u -> u.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            pageAuth.setUser(user);
        }
        
        return pageAuthorizationRepository.save(pageAuth);
    }

    @Transactional
    public void delete(Long id) {
        PageAuthorization pageAuth = findById(id);
        pageAuthorizationRepository.delete(pageAuth);
    }
}

