-- V4__Align_schema_with_current_user_entity.sql
-- Align schema with current User entity model and remove STAFF role (safe + idempotent)

-- 1) Drop unused columns (from old V1 schema) if they exist
ALTER TABLE users DROP COLUMN IF EXISTS first_name;
ALTER TABLE users DROP COLUMN IF EXISTS last_name;

-- 2) Drop multi-role table (we use a single role column in users)
DROP TABLE IF EXISTS user_roles;

-- 3) Ensure email is NOT NULL (entity + API require it)
ALTER TABLE users ALTER COLUMN email SET NOT NULL;

-- 4) Ensure role is NOT NULL and clean legacy STAFF values
UPDATE users SET role = 'GUARDIAN' WHERE role = 'STAFF';
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- If old migrations set a default (e.g., STAFF), drop it to avoid hidden behavior
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;

-- 5) Add a DB-level check constraint for allowed roles (only if not already present)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'check_users_role'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT check_users_role
            CHECK (role IN (
                'ADMIN', 'HR', 'RECEPTIONIST', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'GUARDIAN'
            ));
    END IF;
END $$;

-- 6) Documentation
COMMENT ON TABLE users IS 'User accounts with simplified role-based access';