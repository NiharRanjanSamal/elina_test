package com.elina.authorization.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.User;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.UserRepository;
import com.elina.authorization.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user management with tenant-aware operations.
 * 
 * Tenant enforcement: All user operations automatically filter by tenant_id
 * from TenantContext. Users can only be managed within their own tenant.
 * 
 * To reuse in other systems: Ensure TenantContext is set before calling
 * any service methods that query users.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return userRepository.findAll().stream()
                .filter(user -> user.getTenant().getId().equals(tenantId))
                .toList();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User create(User user, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        
        user.setTenant(tenant);
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, User userDetails) {
        Long tenantId = TenantContext.getTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmail(userDetails.getEmail());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setIsActive(userDetails.getIsActive());

        if (userDetails.getPasswordHash() != null && !userDetails.getPasswordHash().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(userDetails.getPasswordHash()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }
}

