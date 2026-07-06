-- V3__Create_Staff_Users.sql
-- Create table for backend staff users and seed default admin account

CREATE TABLE IF NOT EXISTS staff_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    role VARCHAR(30),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin account with BCrypt hashed password for 'Admin@123'
INSERT INTO staff_users (username, password, full_name, role, enabled)
VALUES ('admin', '$2a$10$f/9M1tKz.gPj.4XgC/W2dOxz3Dq5mQ0z8ZJtQZf/kR.V954b4.V1G', 'Administrator', 'ADMIN', TRUE)
ON CONFLICT (username) DO NOTHING;
