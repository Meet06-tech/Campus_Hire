-- Records the actual event time when a final SELECTED outcome is saved.
-- Existing applications remain NULL because their historical selection time is unknown.
ALTER TABLE placement_applications
    ADD COLUMN placement_selected_at TIMESTAMPTZ;
