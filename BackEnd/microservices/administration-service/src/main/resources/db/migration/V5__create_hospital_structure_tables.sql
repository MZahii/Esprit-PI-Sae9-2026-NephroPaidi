CREATE TABLE IF NOT EXISTS hospital_floors (
    id BIGSERIAL PRIMARY KEY,
    floor_order INTEGER NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_hospital_floors_floor_order UNIQUE (floor_order),
    CONSTRAINT chk_hospital_floors_floor_order_non_negative CHECK (floor_order >= 0)
);

CREATE TABLE IF NOT EXISTS floor_workspaces (
    id BIGSERIAL PRIMARY KEY,
    floor_id BIGINT NOT NULL,
    workspace_type VARCHAR(40) NOT NULL,
    sequence_number INTEGER NOT NULL,
    workspace_code VARCHAR(80) NOT NULL,
    workspace_name VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_floor_workspaces_floor FOREIGN KEY (floor_id) REFERENCES hospital_floors(id) ON DELETE CASCADE,
    CONSTRAINT chk_floor_workspaces_sequence_positive CHECK (sequence_number > 0),
    CONSTRAINT chk_floor_workspaces_type CHECK (workspace_type IN (
        'ADMIN_OFFICE',
        'HR_OFFICE',
        'DOCTOR_OFFICE',
        'HOSPITALIZATION_ROOM',
        'DIALYSIS_ROOM',
        'SURGERY_ROOM',
        'LABORATORY',
        'PHARMACY',
        'RECEPTION',
        'DEPARTMENT',
        'OTHER'
    ))
);

CREATE INDEX IF NOT EXISTS idx_floor_workspaces_floor_id ON floor_workspaces(floor_id);
CREATE INDEX IF NOT EXISTS idx_floor_workspaces_floor_type_seq ON floor_workspaces(floor_id, workspace_type, sequence_number);
CREATE INDEX IF NOT EXISTS idx_floor_workspaces_name ON floor_workspaces(workspace_name);
