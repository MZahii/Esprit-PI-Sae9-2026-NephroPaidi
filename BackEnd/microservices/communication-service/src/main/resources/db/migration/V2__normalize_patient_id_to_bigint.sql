DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'follow_up_messages'
          AND column_name = 'patient_id'
          AND udt_name = 'uuid'
    ) THEN
        TRUNCATE TABLE message_audit_logs, message_replies, follow_up_messages, appointment_requests;

        ALTER TABLE follow_up_messages
            ALTER COLUMN patient_id TYPE BIGINT USING NULL::BIGINT;

        ALTER TABLE appointment_requests
            ALTER COLUMN patient_id TYPE BIGINT USING NULL::BIGINT;
    END IF;
END;
$$;