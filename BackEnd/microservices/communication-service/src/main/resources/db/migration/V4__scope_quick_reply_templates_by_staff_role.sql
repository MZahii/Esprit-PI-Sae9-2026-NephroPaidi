ALTER TABLE quick_reply_templates
    ADD COLUMN IF NOT EXISTS staff_role VARCHAR(20);

UPDATE quick_reply_templates
SET staff_role = CASE
    WHEN message_type IN ('ADMINISTRATIVE', 'APPOINTMENT', 'QUESTION', 'COMPLAINT') THEN 'RECEPTIONIST'
    WHEN message_type IN ('MEDICAL', 'LAB_RESULT', 'OTHER') THEN 'NURSE'
    ELSE 'RECEPTIONIST'
END
WHERE staff_role IS NULL;

ALTER TABLE quick_reply_templates
    ALTER COLUMN staff_role SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_quick_reply_templates_staff_role
    ON quick_reply_templates(staff_role);

CREATE INDEX IF NOT EXISTS idx_quick_reply_templates_staff_role_message_type
    ON quick_reply_templates(staff_role, message_type);
