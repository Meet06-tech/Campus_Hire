-- Companies and placement_drives already exist in the current database.
-- This migration only extends placement_drives and creates missing V1 tables.

ALTER TABLE placement_drives
    ADD COLUMN department_id BIGINT,
    ADD COLUMN minimum_cgpa NUMERIC(4,2),
    ADD COLUMN graduation_year SMALLINT,
    ADD COLUMN maximum_active_backlogs INTEGER;

ALTER TABLE placement_drives
    ADD CONSTRAINT placement_drives_department_fk
        FOREIGN KEY (department_id) REFERENCES departments(department_id),
    ADD CONSTRAINT placement_drives_cgpa_ck
        CHECK (minimum_cgpa IS NULL OR (minimum_cgpa >= 0 AND minimum_cgpa <= 10)),
    ADD CONSTRAINT placement_drives_backlogs_ck
        CHECK (maximum_active_backlogs IS NULL OR maximum_active_backlogs >= 0);

ALTER TABLE placement_drives DROP CONSTRAINT drives_status_ck;
ALTER TABLE placement_drives
    ADD CONSTRAINT drives_status_ck
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'CANCELLED', 'ARCHIVED'));

CREATE INDEX placement_drives_department_idx ON placement_drives(department_id);
CREATE INDEX placement_drives_status_deadline_idx ON placement_drives(status, application_closes_at);

CREATE TABLE placement_programs (
    program_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    program_code VARCHAR(80) NOT NULL UNIQUE,
    program_name VARCHAR(200) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE placement_drive_programs (
    drive_id BIGINT NOT NULL REFERENCES placement_drives(drive_id) ON DELETE CASCADE,
    program_id BIGINT NOT NULL REFERENCES placement_programs(program_id),
    PRIMARY KEY (drive_id, program_id)
);

CREATE TABLE placement_applications (
    application_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drive_id BIGINT NOT NULL REFERENCES placement_drives(drive_id),
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    CONSTRAINT placement_applications_status_ck
        CHECK (status IN ('APPLIED', 'SHORTLISTED', 'REJECTED', 'SELECTED', 'WITHDRAWN')),
    CONSTRAINT placement_applications_drive_student_uq UNIQUE (drive_id, student_id)
);

CREATE INDEX placement_drive_programs_program_idx ON placement_drive_programs(program_id);
CREATE INDEX placement_applications_student_idx ON placement_applications(student_id);
