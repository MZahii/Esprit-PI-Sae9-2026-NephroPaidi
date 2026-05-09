CREATE TABLE IF NOT EXISTS staff_contracts (
    id BIGSERIAL PRIMARY KEY,
    staff_user_id BIGINT NOT NULL,
    contract_reference VARCHAR(80) NOT NULL UNIQUE,
    contract_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    job_title VARCHAR(120) NOT NULL,
    department VARCHAR(120),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    salary NUMERIC(14, 2),
    currency VARCHAR(10),
    hours_per_week INTEGER,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_staff_contracts_staff_user_id
    ON staff_contracts (staff_user_id);

CREATE INDEX IF NOT EXISTS idx_staff_contracts_status
    ON staff_contracts (status);
