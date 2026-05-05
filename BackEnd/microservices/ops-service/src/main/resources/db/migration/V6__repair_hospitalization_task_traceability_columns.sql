ALTER TABLE hospitalization_task
    ADD COLUMN IF NOT EXISTS last_updated_by_nurse_display_name VARCHAR(160);

UPDATE hospitalization_task
SET last_updated_by_nurse_display_name = last_updated_by_nurse_username
WHERE last_updated_by_nurse_display_name IS NULL
  AND last_updated_by_nurse_username IS NOT NULL;

ALTER TABLE hospitalization_task_execution
    ADD COLUMN IF NOT EXISTS action_performed VARCHAR(40);

UPDATE hospitalization_task_execution
SET action_performed = CASE
    WHEN status = 'DONE' THEN 'COMPLETED'
    WHEN status = 'NOT_DONE' THEN 'MARKED_NOT_DONE'
    ELSE 'UPDATED'
END
WHERE action_performed IS NULL;

ALTER TABLE hospitalization_task_execution
    ALTER COLUMN action_performed SET NOT NULL;

ALTER TABLE hospitalization_task_execution
    ADD COLUMN IF NOT EXISTS nurse_display_name VARCHAR(160);

UPDATE hospitalization_task_execution
SET nurse_display_name = nurse_username
WHERE nurse_display_name IS NULL
  AND nurse_username IS NOT NULL;
