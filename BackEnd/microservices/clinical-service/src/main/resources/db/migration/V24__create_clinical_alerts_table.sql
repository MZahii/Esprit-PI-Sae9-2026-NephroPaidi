-- V24: Create Clinical Alerts Table
-- Purpose: AI-generated clinical recommendations and manual alerts for patient monitoring

CREATE TABLE IF NOT EXISTS clinical_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    alert_type VARCHAR(100) NOT NULL, -- AI_RECOMMENDATION, BP_ABNORMAL, ELECTROLYTE, etc.
    severity VARCHAR(50) NOT NULL, -- CRITICAL, URGENT, WARNING, INFO
    message TEXT NOT NULL,
    details TEXT, -- JSON or detailed explanation
    
    -- Alert state tracking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP,
    resolved BOOLEAN DEFAULT false,
    resolved_at TIMESTAMP,
    
    CONSTRAINT fk_alert_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alert_patient_id ON clinical_alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_alert_severity ON clinical_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alert_resolved ON clinical_alerts(resolved);
CREATE INDEX IF NOT EXISTS idx_alert_created_at ON clinical_alerts(created_at);
CREATE INDEX IF NOT EXISTS idx_alert_type ON clinical_alerts(alert_type);
