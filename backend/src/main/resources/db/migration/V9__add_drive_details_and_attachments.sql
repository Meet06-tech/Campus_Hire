-- Additive placement-drive details. All new fields are optional for existing and new drives.
ALTER TABLE placement_drives
    ADD COLUMN employment_type VARCHAR(80),
    ADD COLUMN position_type VARCHAR(80),
    ADD COLUMN ctc_amount NUMERIC(14,2),
    ADD COLUMN ctc_currency VARCHAR(3),
    ADD COLUMN stipend_type VARCHAR(80),
    ADD COLUMN stipend_min_amount NUMERIC(14,2),
    ADD COLUMN stipend_max_amount NUMERIC(14,2),
    ADD COLUMN stipend_currency VARCHAR(3),
    ADD COLUMN stipend_payment_period VARCHAR(80),
    ADD COLUMN other_benefits TEXT,
    ADD COLUMN bond_details TEXT;

ALTER TABLE placement_drives
    ADD CONSTRAINT placement_drives_compensation_amount_ck CHECK (
        (ctc_amount IS NULL OR ctc_amount >= 0)
        AND (stipend_min_amount IS NULL OR stipend_min_amount >= 0)
        AND (stipend_max_amount IS NULL OR stipend_max_amount >= 0)
        AND (stipend_min_amount IS NULL OR stipend_max_amount IS NULL OR stipend_min_amount <= stipend_max_amount)
    );

ALTER TABLE placement_drives DROP CONSTRAINT drives_status_ck;
ALTER TABLE placement_drives ADD CONSTRAINT drives_status_ck
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'CANCELLED', 'COMPLETED', 'ARCHIVED'));

CREATE TABLE placement_drive_attachments (
    attachment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drive_id BIGINT NOT NULL REFERENCES placement_drives(drive_id) ON DELETE CASCADE,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX placement_drive_attachments_drive_idx ON placement_drive_attachments(drive_id);
