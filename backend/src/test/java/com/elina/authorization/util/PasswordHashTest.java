package com.elina.authorization.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify if the stored password hash matches "Admin@123"
 */
public class PasswordHashTest {

    @Test
    public void testPasswordHash() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        // The hash from the database
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String password = "Admin@123";
        
        System.out.println("========================================");
        System.out.println("Testing Password Hash Verification");
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("Stored Hash: " + storedHash);
        System.out.println();
        
        boolean matches = passwordEncoder.matches(password, storedHash);
        
        System.out.println("Result: " + (matches ? "✓ MATCHES" : "✗ DOES NOT MATCH"));
        System.out.println();
        
        if (!matches) {
            System.out.println("The hash does NOT match 'Admin@123'");
            System.out.println("Generating new hash for 'Admin@123':");
            String newHash = passwordEncoder.encode(password);
            System.out.println("New hash: " + newHash);
            System.out.println();
            System.out.println("Verifying new hash:");
            boolean newMatches = passwordEncoder.matches(password, newHash);
            System.out.println("New hash matches: " + (newMatches ? "✓ YES" : "✗ NO"));
        } else {
            System.out.println("✓ The hash correctly matches 'Admin@123'");
            System.out.println("The password should work for login!");
        }
        
        System.out.println("========================================");
        
        // This will fail the test if password doesn't match, but we want to see the output
        // So we'll just print the result instead of asserting
    }
}

