package com.elina.authorization.controller;

import com.elina.authorization.entity.UserAuthorization;
import com.elina.authorization.service.UserAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User authorization (object-level) management controller with tenant-aware CRUD operations.
 * 
 * Tenant enforcement: All operations automatically filter by tenant_id
 * from TenantContext. User authorizations are tenant-specific.
 */
@RestController
@RequestMapping("/api/user-authorizations")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class UserAuthorizationController {

    private final UserAuthorizationService userAuthorizationService;

    public UserAuthorizationController(UserAuthorizationService userAuthorizationService) {
        this.userAuthorizationService = userAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<List<UserAuthorization>> getAllUserAuthorizations() {
        List<UserAuthorization> userAuthorizations = userAuthorizationService.findAll();
        return ResponseEntity.ok(userAuthorizations);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAuthorization>> getUserAuthorizationsByUserId(@PathVariable Long userId) {
        List<UserAuthorization> userAuthorizations = userAuthorizationService.findByUserId(userId);
        return ResponseEntity.ok(userAuthorizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAuthorization> getUserAuthorizationById(@PathVariable Long id) {
        UserAuthorization userAuth = userAuthorizationService.findById(id);
        return ResponseEntity.ok(userAuth);
    }

    @PostMapping
    public ResponseEntity<UserAuthorization> createUserAuthorization(@RequestBody UserAuthorization userAuth) {
        UserAuthorization createdUserAuth = userAuthorizationService.create(userAuth);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUserAuth);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAuthorization> updateUserAuthorization(@PathVariable Long id, @RequestBody UserAuthorization userAuth) {
        UserAuthorization updatedUserAuth = userAuthorizationService.update(id, userAuth);
        return ResponseEntity.ok(updatedUserAuth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAuthorization(@PathVariable Long id) {
        userAuthorizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

