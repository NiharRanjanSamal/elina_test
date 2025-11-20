package com.elina.authorization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Page authorization entity for page-level access control.
 * Defines which roles/users can access specific pages/routes.
 * 
 * Tenant enforcement: PageAuthorizations are tenant-specific via role/user associations.
 */
@Entity
@Table(name = "page_authorizations", indexes = {
    @Index(name = "idx_page_auth_role", columnList = "role_id,page_path"),
    @Index(name = "idx_page_auth_user", columnList = "user_id,page_path")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_path", nullable = false, length = 255)
    private String pagePath;

    @Column(name = "page_name", length = 200)
    private String pageName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_allowed", nullable = false)
    private Boolean isAllowed = true;

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

