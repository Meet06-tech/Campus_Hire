ALTER TABLE placement_programs
    ADD COLUMN duration_semesters SMALLINT;

ALTER TABLE placement_programs
    ADD CONSTRAINT placement_programs_duration_ck
        CHECK (duration_semesters IS NULL OR duration_semesters > 0);

CREATE TABLE student_skills (
    student_id BIGINT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL,
    CONSTRAINT student_skills_pk PRIMARY KEY (student_id, skill_name),
    CONSTRAINT student_skills_name_ck CHECK (length(btrim(skill_name)) BETWEEN 1 AND 100)
);

CREATE INDEX student_skills_skill_idx ON student_skills(skill_name);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE student_skills TO placement_app;
