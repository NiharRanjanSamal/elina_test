-- Password Reset SQL Script
-- This script updates a user's password hash in the database
-- 
-- IMPORTANT: Replace the values below with your actual values:
-- 1. Replace 'your-email@example.com' with the user's email
-- 2. Replace 'YourNewPassword123' with the desired password
-- 3. The password hash will be generated using BCrypt
--
-- Note: You'll need to generate a BCrypt hash for the password.
-- You can use the reset-password API endpoint instead, which handles hashing automatically.

-- Example: Update password for admin@example.com
-- The BCrypt hash for "Admin@123" is: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

-- To generate a new BCrypt hash, you can:
-- 1. Use the /api/auth/reset-password endpoint (recommended)
-- 2. Use an online BCrypt generator (for testing only)
-- 3. Use a Java utility to generate the hash

-- Example SQL (replace with your values):
/*
UPDATE users 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@example.com' 
  AND tenant_id = (SELECT id FROM tenants WHERE tenant_code = 'DEFAULT');
*/

-- To check the current password hash:
/*
SELECT u.email, u.password_hash, t.tenant_code
FROM users u
JOIN tenants t ON u.tenant_id = t.id
WHERE u.email = 'your-email@example.com';
*/

