package com.elina.authorization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Junction entity for User-Permission many-to-many relationship.
 * Allows direct permission assignment to users (in addition to role-based permissions).
 * 
 * Tenant enforcement: UserPermission associations are tenant-scoped via User entity.
 */
@Entity
@Table(name = "user_permissions", indexes = {
    @Index(name = "idx_user_permission_unique", columnList = "user_id,permission_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

