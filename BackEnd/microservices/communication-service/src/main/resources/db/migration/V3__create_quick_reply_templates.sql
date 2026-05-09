CREATE TABLE IF NOT EXISTS quick_reply_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    template_text VARCHAR(2000) NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quick_reply_templates_message_type
    ON quick_reply_templates(message_type);

CREATE INDEX IF NOT EXISTS idx_quick_reply_templates_name
    ON quick_reply_templates(name);
