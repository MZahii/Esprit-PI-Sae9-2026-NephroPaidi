-- V4__add_appointment_fk_to_consultation.sql
-- Adds foreign key constraint from consultation.appointment_id to appointment table
-- This ensures referential integrity between consultations and appointments

-- First, check if we need to handle any orphaned consultations
-- (consultations with appointment_id that don't exist in appointment table)
-- These will need to have appointment_id set to NULL to allow FK constraint

UPDATE consultation 
SET appointment_id = NULL 
WHERE appointment_id IS NOT NULL 
AND appointment_id NOT IN (SELECT id FROM appointment);

-- Add the foreign key constraint
ALTER TABLE consultation 
ADD CONSTRAINT fk_consultation_appointment 
FOREIGN KEY (appointment_id) REFERENCES appointment(id) 
ON DELETE SET NULL ON UPDATE CASCADE;

-- Create index for better query performance (if not already exists)
CREATE INDEX IF NOT EXISTS idx_consultation_appointment_id ON consultation(appointment_id);
