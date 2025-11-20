package com.elina.authorization.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class to verify password hashes.
 * This can be run as a standalone test to verify password matching.
 */
public class PasswordVerifier {
    
    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        // The hash from the database
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String password = "Admin@123";
        
        System.out.println("Testing password verification:");
        System.out.println("Password: " + password);
        System.out.println("Stored Hash: " + storedHash);
        System.out.println();
        
        boolean matches = passwordEncoder.matches(password, storedHash);
        System.out.println("Password matches: " + matches);
        
        if (!matches) {
            System.out.println("\nPassword does NOT match!");
            System.out.println("Generating new hash for 'Admin@123':");
            String newHash = passwordEncoder.encode(password);
            System.out.println("New hash: " + newHash);
        } else {
            System.out.println("\n✓ Password matches correctly!");
        }
    }
}

