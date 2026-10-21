CREATE TABLE department_email_codes (
    department_email_code_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    department_id BIGINT NOT NULL,
    email_code VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT department_email_codes_department_fk
        FOREIGN KEY (department_id) REFERENCES departments (department_id),
    CONSTRAINT department_email_codes_email_code_uq
        UNIQUE (email_code),
    CONSTRAINT department_email_codes_email_code_ck
        CHECK (email_code = UPPER(email_code) AND email_code ~ '^[A-Z0-9]+$')
);

CREATE INDEX department_email_codes_department_idx
    ON department_email_codes (department_id);
