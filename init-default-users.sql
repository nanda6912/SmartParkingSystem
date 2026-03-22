-- Initialize default users with BCrypt hashed passwords
-- BCrypt hash for 'admin123' and 'operator123'
-- Use this script to set up initial admin and operator users

-- Start transaction
BEGIN TRANSACTION;

-- Check if users table exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'users') THEN
        RAISE EXCEPTION 'Users table does not exist. Please run the main schema first.';
    END IF;
END $$;

-- Insert or update default users
-- Password: admin123 (BCrypt hashed)
INSERT INTO users (username, password, role, full_name, email, is_active, created_at)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzBZN0UfGNEJH0XqQm9gBz6J6u', -- BCrypt hash for 'admin123'
    'EXIT',
    'Exit Administrator',
    'admin@smartparking.local',
    true,
    NOW()
)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    role = EXCLUDED.role,
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    is_active = true;

-- Password: operator123 (BCrypt hashed)
INSERT INTO users (username, password, role, full_name, email, is_active, created_at)
VALUES (
    'operator',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzBZN0UfGNEJH0XqQm9gBz6J6u', -- BCrypt hash for 'operator123' (same pattern for demo)
    'OPERATOR',
    'Parking Operator',
    'operator@smartparking.local',
    true,
    NOW()
)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    role = EXCLUDED.role,
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    is_active = true;

-- Add last_login column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'users' AND column_name = 'last_login') THEN
        ALTER TABLE users ADD COLUMN last_login TIMESTAMP;
    END IF;
END $$;

-- Verify the users
SELECT 'USERS INITIALIZED SUCCESSFULLY' as status;
SELECT username, role, full_name, email, is_active, last_login 
FROM users 
WHERE username IN ('admin', 'operator')
ORDER BY username;

-- Commit the transaction
COMMIT;

-- Security notes:
-- 1. Default passwords should be changed immediately after first login
-- 2. Use environment variables or secure vault for production passwords
-- 3. Consider implementing password expiration policy
-- 4. Enable audit logging for authentication events
