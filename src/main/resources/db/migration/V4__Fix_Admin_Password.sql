-- V4__Fix_Admin_Password.sql
-- Update the admin user password to the verified BCrypt hash of 'Admin@123' and ensure enabled=TRUE

UPDATE staff_users
SET
    password = '$2a$10$MC7TDkL1mNcXcEy81m1oPej5zjos5Lw64FD8vcMNzetii832MIJly',
    enabled = TRUE
WHERE username = 'admin';
