-- V7__add_lab_agent_role.sql
-- Extend allowed role list with LAB_AGENT

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'check_users_role'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT check_users_role;
    END IF;

    ALTER TABLE users
        ADD CONSTRAINT check_users_role
        CHECK (role IN (
            'ADMIN', 'HR', 'RECEPTIONIST', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'LAB_AGENT', 'GUARDIAN'
        ));
END $$;
