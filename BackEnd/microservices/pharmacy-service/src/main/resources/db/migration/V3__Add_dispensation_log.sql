-- V3__Add_dispensation_log.sql
-- Tracks every dispense event for daily history & date filtering

CREATE TABLE IF NOT EXISTS dispensation_logs (
    id           BIGSERIAL PRIMARY KEY,
    batch_id     BIGINT    NOT NULL,
    quantity     INT       NOT NULL CHECK (quantity > 0),
    dispensed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_disp_log_dispensed_at ON dispensation_logs(dispensed_at);
CREATE INDEX IF NOT EXISTS idx_disp_log_batch_id     ON dispensation_logs(batch_id);

-- Seed: a few dispensation logs for today so the page shows data immediately
DO $$
DECLARE
    b1 BIGINT;
    b2 BIGINT;
BEGIN
    SELECT batch_id INTO b1 FROM batches
      WHERE expiration_date > CURRENT_DATE
      ORDER BY batch_id LIMIT 1;

    SELECT batch_id INTO b2 FROM batches
      WHERE expiration_date > CURRENT_DATE
      ORDER BY batch_id OFFSET 1 LIMIT 1;

    IF b1 IS NOT NULL THEN
        INSERT INTO dispensation_logs (batch_id, quantity, dispensed_at) VALUES
            (b1, 2, NOW() - INTERVAL '4 hours'),
            (b1, 5, NOW() - INTERVAL '2 hours');
    END IF;

    IF b2 IS NOT NULL THEN
        INSERT INTO dispensation_logs (batch_id, quantity, dispensed_at) VALUES
            (b2, 3, NOW() - INTERVAL '30 minutes');
    END IF;
END $$;