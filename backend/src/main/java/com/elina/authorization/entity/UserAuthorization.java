package com.elina.authorization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User authorization entity for object-level access control.
 * Defines fine-grained permissions on specific resources/objects for users.
 * 
 * Tenant enforcement: UserAuthorizations are tenant-scoped via User entity.
 */
@Entity
@Table(name = "user_authorizations", indexes = {
    @Index(name = "idx_user_auth_resource", columnList = "user_id,resource_type,resource_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "permission_type", length = 50)
    private String permissionType; // e.g., READ, WRITE, DELETE, FULL

    @Column(name = "is_allowed", nullable = false)
    private Boolean isAllowed = true;

    @Column(name = "conditions", length = 1000)
    private String conditions; // JSON or query conditions

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

