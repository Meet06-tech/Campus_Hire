CREATE TABLE placement_drive_rounds (
    round_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drive_id BIGINT NOT NULL REFERENCES placement_drives(drive_id) ON DELETE CASCADE,
    round_order INTEGER NOT NULL,
    round_name VARCHAR(160) NOT NULL,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    results_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT placement_drive_rounds_order_ck CHECK (round_order > 0),
    CONSTRAINT placement_drive_rounds_name_ck CHECK (length(btrim(round_name)) > 0),
    CONSTRAINT placement_drive_rounds_drive_order_uq UNIQUE (drive_id, round_order)
);

CREATE INDEX placement_drive_rounds_drive_idx ON placement_drive_rounds(drive_id, round_order);

CREATE TABLE placement_round_results (
    round_result_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id BIGINT NOT NULL REFERENCES placement_drive_rounds(round_id) ON DELETE CASCADE,
    application_id BIGINT NOT NULL REFERENCES placement_applications(application_id) ON DELETE CASCADE,
    result VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    recorded_at TIMESTAMPTZ,
    recorded_by_user_id BIGINT REFERENCES users(user_id),
    CONSTRAINT placement_round_results_result_ck CHECK (result IN ('PENDING', 'PASSED', 'FAILED')),
    CONSTRAINT placement_round_results_round_application_uq UNIQUE (round_id, application_id)
);

CREATE INDEX placement_round_results_application_idx ON placement_round_results(application_id);
