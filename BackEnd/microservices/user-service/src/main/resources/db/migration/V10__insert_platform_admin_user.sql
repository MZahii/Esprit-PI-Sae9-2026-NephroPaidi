-- V10__insert_platform_admin_user.sql
-- Insert platform-admin user into the users table
-- This ensures the platform-admin account from Keycloak realm JSON is synchronized to the database

INSERT INTO users (
    keycloak_id,
    username,
    email,
    first_name,
    last_name,
    cin,
    role,
    account_status,
    enabled,
    preferred_language,
    notifications_enabled,
    theme,
    must_change_password,
    is_deleted,
    created_at,
    updated_at
) VALUES (
    '6eed1ac9-594e-4db1-ba57-ccd6168896cd',
    'platform-admin',
    'platform-admin@nephrospaidi.local',
    'Platform',
    'Admin',
    'ADMIN-001',
    'ADMIN',
    'ACTIVE',
    true,
    'en',
    true,
    'light',
    false,
    false,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO UPDATE
SET 
    keycloak_id = EXCLUDED.keycloak_id,
    email = EXCLUDED.email,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role,
    account_status = EXCLUDED.account_status,
    enabled = EXCLUDED.enabled,
    updated_at = NOW()
WHERE users.username = 'platform-admin';
