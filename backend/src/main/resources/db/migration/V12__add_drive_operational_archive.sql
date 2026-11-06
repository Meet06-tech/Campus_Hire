-- Soft archive for completed/cancelled drives. The original status is retained.
ALTER TABLE placement_drives
    ADD COLUMN operationally_archived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at TIMESTAMPTZ;

CREATE INDEX placement_drives_operational_archive_idx
    ON placement_drives(operationally_archived, status);
