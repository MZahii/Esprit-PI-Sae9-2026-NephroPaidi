-- V8__normalize_users_role_constraint_for_lab_agent.sql
-- Some environments use users_role_check while others use check_users_role.
-- Normalize by dropping both and recreating one canonical constraint.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users DROP CONSTRAINT IF EXISTS check_users_role;

ALTER TABLE users
    ADD CONSTRAINT check_users_role
    CHECK (role IN (
        'ADMIN', 'HR', 'RECEPTIONIST', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'LAB_AGENT', 'GUARDIAN'
    ));
