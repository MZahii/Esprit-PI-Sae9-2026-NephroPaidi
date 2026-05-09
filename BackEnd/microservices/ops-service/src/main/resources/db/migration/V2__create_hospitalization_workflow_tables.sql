CREATE TABLE IF NOT EXISTS hospitalization_case (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    consultation_id UUID,
    doctor_keycloak_id VARCHAR(128) NOT NULL,
    doctor_username VARCHAR(120) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hospitalization_task (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalization_case(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    instructions VARCHAR(1000),
    measurement_kind VARCHAR(20) NOT NULL,
    expected_unit VARCHAR(32),
    display_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    latest_note VARCHAR(2000),
    latest_numeric_value NUMERIC(10, 2),
    latest_text_value VARCHAR(255),
    latest_unit VARCHAR(32),
    last_updated_by_nurse_id VARCHAR(128),
    last_updated_by_nurse_username VARCHAR(120),
    last_updated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hospitalization_task_execution (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalization_case(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES hospitalization_task(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    nurse_keycloak_id VARCHAR(128) NOT NULL,
    nurse_username VARCHAR(120) NOT NULL,
    note VARCHAR(2000),
    numeric_value NUMERIC(10, 2),
    text_value VARCHAR(255),
    unit VARCHAR(32),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hospitalization_case_patient_id ON hospitalization_case(patient_id);
CREATE INDEX IF NOT EXISTS idx_hospitalization_case_status ON hospitalization_case(status);
CREATE INDEX IF NOT EXISTS idx_hospitalization_task_hospitalization_id ON hospitalization_task(hospitalization_id);
CREATE INDEX IF NOT EXISTS idx_hospitalization_task_execution_task_id ON hospitalization_task_execution(task_id);
CREATE INDEX IF NOT EXISTS idx_hospitalization_task_execution_hospitalization_id ON hospitalization_task_execution(hospitalization_id);
