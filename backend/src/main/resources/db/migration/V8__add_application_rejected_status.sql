-- Distinguishes staff application rejection from the final REJECTED outcome.
ALTER TABLE placement_applications
    DROP CONSTRAINT placement_applications_status_ck;

ALTER TABLE placement_applications
    ADD CONSTRAINT placement_applications_status_ck
        CHECK (status IN ('APPLIED', 'SHORTLISTED', 'APPLICATION_REJECTED', 'REJECTED', 'SELECTED', 'WITHDRAWN'));
