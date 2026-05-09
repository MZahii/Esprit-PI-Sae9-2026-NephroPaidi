-- V1__Create_pharmacy_schema.sql
-- NephrosPaidi – Pharmacy Service initial schema

CREATE TABLE IF NOT EXISTS medications (
    medication_id   BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    form            VARCHAR(100),
    pediatric_dosage VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS batches (
    batch_id        BIGSERIAL PRIMARY KEY,
    batch_number    VARCHAR(100) NOT NULL UNIQUE,
    manufacture_date DATE,
    expiration_date  DATE NOT NULL,
    quantity        INT  NOT NULL CHECK (quantity >= 0),
    medication_id   BIGINT NOT NULL REFERENCES medications(medication_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id  BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    contact_info TEXT
);

CREATE TABLE IF NOT EXISTS supply_orders (
    order_id          BIGSERIAL PRIMARY KEY,
    supplier_id       BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    medication_id     BIGINT NOT NULL,
    order_date        DATE NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','DELIVERED','CANCELLED')),
    ordered_quantity  INT NOT NULL CHECK (ordered_quantity > 0)
);

CREATE TABLE IF NOT EXISTS stocks (
    stock_id           BIGSERIAL PRIMARY KEY,
    batch_id           BIGINT NOT NULL UNIQUE REFERENCES batches(batch_id) ON DELETE CASCADE,
    quantity_available INT    NOT NULL CHECK (quantity_available >= 0)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_batches_medication ON batches(medication_id);
CREATE INDEX IF NOT EXISTS idx_batches_expiration ON batches(expiration_date);
CREATE INDEX IF NOT EXISTS idx_supply_orders_supplier ON supply_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supply_orders_status ON supply_orders(status);
CREATE INDEX IF NOT EXISTS idx_stocks_batch ON stocks(batch_id);
