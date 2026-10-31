ALTER TABLE placement_applications
    ADD COLUMN offer_status VARCHAR(16),
    ADD COLUMN offer_decided_at TIMESTAMPTZ;

ALTER TABLE placement_applications
    ADD CONSTRAINT placement_applications_offer_status_ck
        CHECK (offer_status IS NULL OR offer_status IN ('ACCEPTED', 'DECLINED'));

CREATE INDEX placement_applications_student_offer_status_idx
    ON placement_applications(student_id, offer_status);
