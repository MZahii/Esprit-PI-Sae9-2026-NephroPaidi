CREATE TABLE IF NOT EXISTS surgical_predictions (
    id BIGSERIAL PRIMARY KEY,
    surgical_case_id BIGINT NOT NULL,
    phase VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    prediction_label VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    probability DOUBLE PRECISION NOT NULL,
    recommendation TEXT,
    input_snapshot_json TEXT,
    output_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_surgical_predictions_case
        FOREIGN KEY (surgical_case_id) REFERENCES surgical_cases(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_surgical_predictions_case_id ON surgical_predictions(surgical_case_id);
CREATE INDEX IF NOT EXISTS idx_surgical_predictions_phase ON surgical_predictions(phase);
