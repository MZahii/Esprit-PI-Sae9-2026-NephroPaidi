ALTER TABLE lab_requests
    ADD COLUMN IF NOT EXISTS test_items_json TEXT;

ALTER TABLE lab_results
    ADD COLUMN IF NOT EXISTS test_item_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS test_item_label VARCHAR(255);
