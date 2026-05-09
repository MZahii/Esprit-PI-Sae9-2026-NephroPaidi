CREATE TABLE IF NOT EXISTS staff_workspace_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(40) NOT NULL,
    workspace_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_staff_workspace_assignments_workspace
        FOREIGN KEY (workspace_id) REFERENCES floor_workspaces(id) ON DELETE CASCADE,
    CONSTRAINT uk_staff_workspace_assignments_user_workspace UNIQUE (user_id, workspace_id),
    CONSTRAINT chk_staff_workspace_assignments_role CHECK (role IN (
        'ADMIN',
        'HR',
        'DOCTOR',
        'LAB_AGENT',
        'PHARMACIST',
        'RECEPTIONIST'
    ))
);

CREATE INDEX IF NOT EXISTS idx_staff_workspace_assignments_role ON staff_workspace_assignments(role);
CREATE INDEX IF NOT EXISTS idx_staff_workspace_assignments_workspace_role ON staff_workspace_assignments(workspace_id, role);
CREATE INDEX IF NOT EXISTS idx_staff_workspace_assignments_user_role ON staff_workspace_assignments(user_id, role);
