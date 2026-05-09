-- V6__guardian_notifications_and_sections.sql
-- Guardian notifications + consultation sections

CREATE TABLE guardian_notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guardian_user_id BIGINT NOT NULL,
    consultation_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    read_at TIMESTAMP
);

CREATE INDEX idx_guardian_notification_guardian ON guardian_notification(guardian_user_id);
CREATE INDEX idx_guardian_notification_consultation ON guardian_notification(consultation_id);

CREATE TABLE consultation_section (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL,
    section_type VARCHAR(32) NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_consultation_section UNIQUE (consultation_id, section_type),
    CONSTRAINT fk_consultation_section_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation(id) ON DELETE CASCADE
);

CREATE INDEX idx_consultation_section_consultation_id ON consultation_section(consultation_id);
