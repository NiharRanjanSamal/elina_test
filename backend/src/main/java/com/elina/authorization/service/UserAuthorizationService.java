package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.User;
import com.elina.authorization.entity.UserAuthorization;
import com.elina.authorization.repository.UserAuthorizationRepository;
import com.elina.authorization.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user authorization (object-level) management with tenant-aware operations.
 * 
 * Tenant enforcement: All user authorization operations automatically filter
 * by tenant_id from TenantContext. User authorizations are tenant-specific.
 */
@Service
public class UserAuthorizationService {

    private final UserAuthorizationRepository userAuthorizationRepository;
    private final UserRepository userRepository;

    public UserAuthorizationService(
            UserAuthorizationRepository userAuthorizationRepository,
            UserRepository userRepository) {
        this.userAuthorizationRepository = userAuthorizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserAuthorization> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return userAuthorizationRepository.findAll().stream()
                .filter(ua -> ua.getUser().getTenant().getId().equals(tenantId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserAuthorization> findByUserId(Long userId) {
        Long tenantId = TenantContext.getTenantId();
        // Verify user exists and belongs to tenant
        userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userAuthorizationRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    @Transactional(readOnly = true)
    public UserAuthorization findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return userAuthorizationRepository.findById(id)
                .filter(ua -> ua.getUser().getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("User authorization not found"));
    }

    @Transactional
    public UserAuthorization create(UserAuthorization userAuth) {
        Long tenantId = TenantContext.getTenantId();
        User user = userRepository.findById(userAuth.getUser().getId())
                .filter(u -> u.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userAuth.setUser(user);
        return userAuthorizationRepository.save(userAuth);
    }

    @Transactional
    public UserAuthorization update(Long id, UserAuthorization userAuthDetails) {
        Long tenantId = TenantContext.getTenantId();
        UserAuthorization userAuth = findById(id);
        
        userAuth.setResourceType(userAuthDetails.getResourceType());
        userAuth.setResourceId(userAuthDetails.getResourceId());
        userAuth.setPermissionType(userAuthDetails.getPermissionType());
        userAuth.setIsAllowed(userAuthDetails.getIsAllowed());
        userAuth.setConditions(userAuthDetails.getConditions());
        
        if (userAuthDetails.getUser() != null) {
            User user = userRepository.findById(userAuthDetails.getUser().getId())
                    .filter(u -> u.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            userAuth.setUser(user);
        }
        
        return userAuthorizationRepository.save(userAuth);
    }

    @Transactional
    public void delete(Long id) {
        UserAuthorization userAuth = findById(id);
        userAuthorizationRepository.delete(userAuth);
    }
}

