DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'hospital_equipment'
          AND column_name = 'status'
    ) THEN
        UPDATE hospital_equipment
        SET status = 'AVAILABLE'
        WHERE status IN ('IN_USE', 'RESERVED')
          AND COALESCE(is_archived, FALSE) = FALSE;

        UPDATE hospital_equipment
        SET status = 'ARCHIVED',
            is_archived = TRUE,
            archived_at = COALESCE(archived_at, NOW())
        WHERE COALESCE(is_archived, FALSE) = TRUE
           OR status = 'ARCHIVED';

        ALTER TABLE hospital_equipment DROP CONSTRAINT IF EXISTS chk_hospital_equipment_status;
        ALTER TABLE hospital_equipment
            ADD CONSTRAINT chk_hospital_equipment_status
            CHECK (status IN ('AVAILABLE', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE', 'ARCHIVED'));
    END IF;
END $$;
