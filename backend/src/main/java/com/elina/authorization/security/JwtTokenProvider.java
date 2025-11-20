package com.elina.authorization.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * 
 * Tenant enforcement: JWT includes tenant_id, user_id, roles, and permissions.
 * The TenantFilter extracts tenant_id from JWT claims to set TenantContext.
 * 
 * To reuse in other systems: Update claim names if needed and ensure
 * tenant_id is always included in token generation.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Generate JWT token with tenant_id, user_id, roles, and permissions.
     * 
     * Tenant enforcement: tenant_id is included in claims for request-level validation.
     */
    public String generateToken(Long userId, Long tenantId, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenant_id", tenantId)
                .claim("user_id", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate refresh token with longer expiration.
     */
    public String generateRefreshToken(Long userId, Long tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenant_id", tenantId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Get user ID from token.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("user_id", Long.class);
    }

    /**
     * Get tenant ID from token.
     * 
     * Tenant enforcement: This is extracted by TenantFilter to set TenantContext.
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tenant_id", Long.class);
    }

    /**
     * Get roles from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roles", List.class);
    }

    /**
     * Get permissions from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("permissions", List.class);
    }

    /**
     * Validate token and check expiration.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

