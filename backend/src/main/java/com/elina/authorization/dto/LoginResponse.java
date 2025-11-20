package com.elina.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserProfile userProfile;
    private TenantInfo tenantInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private List<String> roles;
        private List<String> permissions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantInfo {
        private Long id;
        private String tenantCode;
        private String name;
        private String clientCode;
    }
}

