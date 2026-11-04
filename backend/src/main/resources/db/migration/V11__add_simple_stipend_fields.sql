-- Introduce simple optional stipend text without removing legacy compensation columns.
ALTER TABLE companies ADD COLUMN stipend VARCHAR(255);
ALTER TABLE placement_drives ADD COLUMN stipend VARCHAR(255);
