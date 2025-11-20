package com.elina.authorization.security;

import com.elina.authorization.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TenantFilter extracts tenant_id from JWT and sets TenantContext + SecurityContext.
 * 
 * Tenant enforcement: This is the critical filter that:
 * 1. Extracts tenant_id from JWT claims
 * 2. Sets TenantContext (ThreadLocal) for @TenantAware services
 * 3. Sets SecurityContext for Spring Security
 * 4. Clears context after request to prevent memory leaks
 * 
 * To reuse in other systems: Ensure this filter runs after JWT validation
 * and before any @TenantAware service calls. Update to match your JWT claim structure.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public TenantFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);

            if (token != null && tokenProvider.validateToken(token) && !tokenProvider.isRefreshToken(token)) {
                // Extract tenant_id from JWT and set TenantContext
                // This ensures all subsequent operations are tenant-scoped
                Long tenantId = tokenProvider.getTenantIdFromToken(token);
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);

                    // Set SecurityContext with user authentication
                    Long userId = tokenProvider.getUserIdFromToken(token);
                    List<String> roles = tokenProvider.getRolesFromToken(token);
                    List<String> permissions = tokenProvider.getPermissionsFromToken(token);
                    
                    // Combine roles and permissions as authorities
                    // Spring Security convention: roles should have ROLE_ prefix
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    if (roles != null) {
                        authorities.addAll(roles.stream()
                                .map(role -> {
                                    // Add ROLE_ prefix if not already present
                                    String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                    return new SimpleGrantedAuthority(roleName);
                                })
                                .collect(Collectors.toList()));
                    }
                    if (permissions != null) {
                        authorities.addAll(permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear TenantContext after request to prevent memory leaks
            // and ensure tenant isolation between requests
            TenantContext.clear();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

