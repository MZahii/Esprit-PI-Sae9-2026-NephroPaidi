UPDATE floor_workspaces
SET workspace_type = 'OFFICE'
WHERE workspace_type IN ('DEPARTMENT', 'OTHER');

ALTER TABLE floor_workspaces
DROP CONSTRAINT IF EXISTS chk_floor_workspaces_type;

ALTER TABLE floor_workspaces
ADD CONSTRAINT chk_floor_workspaces_type CHECK (workspace_type IN (
    'ADMIN_OFFICE',
    'HR_OFFICE',
    'DOCTOR_OFFICE',
    'OFFICE',
    'HOSPITALIZATION_ROOM',
    'DIALYSIS_ROOM',
    'SURGERY_ROOM',
    'LABORATORY',
    'PHARMACY',
    'RECEPTION'
));
