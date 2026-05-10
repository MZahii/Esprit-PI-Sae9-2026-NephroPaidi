-- V26: Align discharge document weight type with JPA mapping

ALTER TABLE discharge_documents
    ALTER COLUMN discharge_weight_g TYPE INTEGER
    USING ROUND(discharge_weight_g)::INTEGER;
