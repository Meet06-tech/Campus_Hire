# Placement domain V1

The V1 placement schema uses `placement_programs` as an empty reference table and
`placement_drive_programs` as its many-to-many link to drives. Program records
must be configured by the university before restricted drives are created.

When a drive has no rows in `placement_drive_programs`, it has no program
restriction. Other rules still apply: the drive must be visible to the
student's department (or be university-wide), be published and within its
deadline, and satisfy any configured CGPA and graduation-year rules.

For a restricted drive, the backend compares `students.academic_program` to
`placement_programs.program_code` after trimming surrounding whitespace. The
comparison is otherwise exact and case-sensitive. Different spelling, casing,
or aliases are not treated as equivalent. No program seed data is included in
the migration.

The current `students` schema does not contain an active-backlog count. A drive
may store `maximum_active_backlogs`, but applications fail closed when that
rule is configured until student backlog data is added through an approved
future schema change.
