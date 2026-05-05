CREATE TABLE IF NOT EXISTS hospital_equipment (
    id BIGSERIAL PRIMARY KEY,
    equipment_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    category VARCHAR(60) NOT NULL,
    subtype VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    description VARCHAR(600),
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP,
    archived_reason VARCHAR(400),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hospital_equipment_category CHECK (category IN (
        'CONSULTATION_BED',
        'HOSPITALIZATION_BED',
        'DIALYSIS_BED',
        'DIALYSIS_MACHINE',
        'SURGERY_TABLE_OR_BED',
        'LABORATORY_EQUIPMENT',
        'IMAGING_EQUIPMENT',
        'ANALYSIS_EQUIPMENT'
    )),
    CONSTRAINT chk_hospital_equipment_status CHECK (status IN (
        'AVAILABLE',
        'IN_USE',
        'UNDER_MAINTENANCE',
        'OUT_OF_SERVICE',
        'RESERVED',
        'ARCHIVED'
    ))
);

CREATE TABLE IF NOT EXISTS equipment_archive_logs (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_equipment_archive_logs_equipment FOREIGN KEY (equipment_id) REFERENCES hospital_equipment(id)
);

CREATE TABLE IF NOT EXISTS equipment_placements (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    placed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    removed_at TIMESTAMP,
    CONSTRAINT fk_equipment_placements_equipment FOREIGN KEY (equipment_id) REFERENCES hospital_equipment(id),
    CONSTRAINT fk_equipment_placements_workspace FOREIGN KEY (workspace_id) REFERENCES floor_workspaces(id)
);

CREATE INDEX IF NOT EXISTS idx_hospital_equipment_category ON hospital_equipment(category);
CREATE INDEX IF NOT EXISTS idx_hospital_equipment_status ON hospital_equipment(status);
CREATE INDEX IF NOT EXISTS idx_hospital_equipment_archived ON hospital_equipment(is_archived);
CREATE INDEX IF NOT EXISTS idx_equipment_placements_equipment_active ON equipment_placements(equipment_id, active);
CREATE INDEX IF NOT EXISTS idx_equipment_placements_workspace_active ON equipment_placements(workspace_id, active);
