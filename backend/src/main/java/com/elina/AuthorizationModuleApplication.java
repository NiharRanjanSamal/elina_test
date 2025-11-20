package com.elina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Authorization Module.
 * 
 * This module provides multi-tenant JWT-based authentication and authorization
 * with tenant isolation and role/permission management.
 * 
 * To reuse this module in other systems:
 * 1. Copy the authorization package structure
 * 2. Ensure database schema is migrated using Liquibase
 * 3. Configure JWT secret in application.yml
 * 4. Update package names to match your project structure
 * 5. Configure CORS settings if needed for frontend integration
 */
@SpringBootApplication
public class AuthorizationModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationModuleApplication.class, args);
    }
}

