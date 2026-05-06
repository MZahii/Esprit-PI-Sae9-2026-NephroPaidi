-- V7__Add_full_pharmacy_workflow.sql
-- Full pharmacy workflow: equipment stock, dialysis stock,
-- stock movements, prescriptions, and supply_orders enhancements

-- ─── 1. Extend supply_orders with delivery discrepancy tracking ───────────────

ALTER TABLE supply_orders
    ADD COLUMN IF NOT EXISTS item_type          VARCHAR(20) NOT NULL DEFAULT 'MEDICATION'
        CHECK (item_type IN ('MEDICATION','EQUIPMENT','DIALYSIS')),
    ADD COLUMN IF NOT EXISTS item_id            BIGINT,
    ADD COLUMN IF NOT EXISTS item_name          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivered_quantity INT CHECK (delivered_quantity >= 0),
    ADD COLUMN IF NOT EXISTS actual_delivery_date DATE;

-- Backfill item_id / item_name from existing medication orders
UPDATE supply_orders SET item_id = medication_id WHERE item_id IS NULL AND medication_id IS NOT NULL;

-- Allow medication_id to be nullable now that item_id covers all types
ALTER TABLE supply_orders
    ALTER COLUMN medication_id DROP NOT NULL;

-- ─── 2. Medical equipment catalog ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS equipment_items (
    item_id         BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    category        VARCHAR(100),
    unit            VARCHAR(50),
    minimum_stock   INT CHECK (minimum_stock >= 0),
    description     TEXT
);

CREATE TABLE IF NOT EXISTS equipment_stock (
    id                  BIGSERIAL PRIMARY KEY,
    item_id             BIGINT NOT NULL UNIQUE REFERENCES equipment_items(item_id) ON DELETE CASCADE,
    quantity_available  INT NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_equipment_stock_item ON equipment_stock(item_id);

-- ─── 3. Dialysis material catalog ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS dialysis_items (
    item_id         BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    category        VARCHAR(100),
    unit            VARCHAR(50),
    minimum_stock   INT CHECK (minimum_stock >= 0),
    description     TEXT
);

CREATE TABLE IF NOT EXISTS dialysis_stock (
    id                  BIGSERIAL PRIMARY KEY,
    item_id             BIGINT NOT NULL UNIQUE REFERENCES dialysis_items(item_id) ON DELETE CASCADE,
    quantity_available  INT NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dialysis_stock_item ON dialysis_stock(item_id);

-- ─── 4. Outgoing stock movement log ──────────────────────────────────────────

CREATE TABLE IF NOT EXISTS stock_movements (
    id                  BIGSERIAL PRIMARY KEY,
    stock_type          VARCHAR(20) NOT NULL CHECK (stock_type IN ('EQUIPMENT','DIALYSIS')),
    item_id             BIGINT NOT NULL,
    item_name           VARCHAR(255) NOT NULL,
    quantity_taken      INT NOT NULL CHECK (quantity_taken > 0),
    requested_by        VARCHAR(255),
    requested_by_role   VARCHAR(100),
    purpose             TEXT,
    taken_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_type     ON stock_movements(stock_type);
CREATE INDEX IF NOT EXISTS idx_stock_movements_item     ON stock_movements(item_id, stock_type);
CREATE INDEX IF NOT EXISTS idx_stock_movements_taken_at ON stock_movements(taken_at DESC);

-- ─── 5. Doctor-to-pharmacy prescriptions ─────────────────────────────────────

CREATE TABLE IF NOT EXISTS pharmacy_prescriptions (
    id                BIGSERIAL PRIMARY KEY,
    consultation_id   VARCHAR(255),
    patient_id        BIGINT,
    patient_name      VARCHAR(255),
    doctor_id         VARCHAR(255),
    doctor_name       VARCHAR(255),
    medications_json  TEXT NOT NULL,
    notes             TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','PROCESSING','DISPENSED','CANCELLED')),
    received_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at      TIMESTAMP,
    processed_by      VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_pharmacy_prescriptions_status     ON pharmacy_prescriptions(status);
CREATE INDEX IF NOT EXISTS idx_pharmacy_prescriptions_patient    ON pharmacy_prescriptions(patient_id);
CREATE INDEX IF NOT EXISTS idx_pharmacy_prescriptions_received   ON pharmacy_prescriptions(received_at DESC);