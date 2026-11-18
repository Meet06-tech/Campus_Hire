ALTER TABLE password_change_otps
    ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'PASSWORD_CHANGE',
    ADD COLUMN recovery_token_hash VARCHAR(64),
    ADD COLUMN recovery_token_expires_at TIMESTAMPTZ;

ALTER TABLE password_change_otps
    ADD CONSTRAINT password_change_otps_purpose_ck
    CHECK (purpose IN ('PASSWORD_CHANGE', 'PASSWORD_RECOVERY'));

CREATE INDEX password_change_otps_recovery_token_idx
    ON password_change_otps (recovery_token_hash)
    WHERE recovery_token_hash IS NOT NULL AND used_at IS NULL;
