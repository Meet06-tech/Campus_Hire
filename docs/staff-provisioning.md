# CampusHire Initial Staff Provisioning

This is a manually invoked, local-only procedure for provisioning exactly one
`UTPO` and one `DTPO` account. The supported path is the explicit
`StaffProvisioningCli`; it is not a startup hook, public endpoint, or normal
student-registration flow.

Do not execute the manual SQL fallback section until the operator has reviewed
all preflight results and explicitly confirmed the two-account provisioning.

## Preconditions

Use the controlled database-owner connection for the provisioning session.
Do not change database roles or permissions. Set the approved allowlist before
invoking the utility:

```bash
export STAFF_ALLOWED_EMAIL_DOMAINS=charusat.ac.in
```

The utility starts the existing Spring configuration in non-web mode and
performs read-only database validation before it can collect input. It requires
the connected effective role to match `PROVISIONING_DB_OWNER`, verifies that
the role owns the `placement_management` database, and verifies ownership of
the `public.users` table. It fails closed if any check fails. Staff identifiers
must be complete email addresses in the exact allowlist; the application’s
`StaffEmailAllowlist` is the validation authority. The utility reads both
passwords without echo and uses the existing Spring `PasswordEncoder`. It never
prints passwords or hashes and does not write files.

Run it from IntelliJ IDEA by running
`com.placementmanagement.tools.StaffProvisioningCli` as a desktop run
configuration with `DB_USERNAME`,
`DB_PASSWORD`, `STAFF_ALLOWED_EMAIL_DOMAINS`, and `PROVISIONING_DB_OWNER` set
in protected environment settings. When `System.console()` is unavailable,
the utility uses IntelliJ’s desktop Swing dialogs and a hidden
`JPasswordField` for passwords. Do not use a headless run configuration; if no
real console or desktop dialogs are available, the utility stops before
collecting staff details or opening a write transaction. Do not type
`DB_PASSWORD` into a shell
command or place it in shell history; use the IDE’s protected environment
configuration or an approved secret mechanism. From the backend directory, the
Spring Boot Maven plugin can invoke the same class while database credentials
are supplied by the environment rather than command-line arguments:

```bash
STAFF_ALLOWED_EMAIL_DOMAINS=charusat.ac.in PROVISIONING_DB_OWNER=<approved-owner> \
mvn -q -DskipTests spring-boot:run \
  -Dspring-boot.run.main-class=com.placementmanagement.tools.StaffProvisioningCli
```

Provide `DB_USERNAME` and `DB_PASSWORD` through the protected environment
mechanism before running that command.

## Secure password handling

The utility connects to PostgreSQL during Spring startup and read-only owner
validation, before confirmation. It does not insert accounts until the exact
confirmation phrase is entered. It collects both passwords interactively with echo disabled and
generates hashes through the application’s existing Spring `PasswordEncoder`
bean (`BCryptPasswordEncoder`). Do not implement a second BCrypt format and do
not use an HTTP endpoint. The utility does not create accounts until the
operator types the exact confirmation phrase shown by the program.

Keep plaintext passwords and generated hashes only in protected process memory
or a protected temporary handoff. Do not put either value in this document,
shell history, a committed SQL file, logs, screenshots, or the final report.

## Read-only preflight

Run these queries as the provisioning owner before any insert. They are
read-only and resolve role IDs by role code rather than assuming numeric IDs:

```sql
SELECT current_database(), current_user;

SELECT role_id, role_code, role_name
FROM roles
WHERE role_code IN ('UTPO', 'DTPO');

SELECT department_id, department_name, is_active
FROM departments
WHERE department_id = 2;

SELECT lower(university_email) AS normalized_university_email
FROM users
WHERE lower(university_email) IN (lower(:'utpo_email'), lower(:'dtpo_email'));
```

Normalize both proposed addresses to lowercase before continuing. Stop if
either role is missing, if department 2 is absent or inactive, if either
normalized address already exists, or if the two normalized addresses are
identical. Do not reuse an existing account for either role.

## Manual SQL fallback (not the preferred utility path)

Before starting `BEGIN`, the operator must explicitly type:
`PROVISION EXACTLY ONE UTPO AND ONE DTPO`. If that confirmation is not given,
stop without opening a write transaction. Run the future psql script with
fail-fast enabled:

```bash
psql --set=ON_ERROR_STOP=1 ...
```

Supply names, normalized emails, and BCrypt hashes through the database
client’s secure parameter mechanism. Do not place hashes or plaintext
passwords in command-line arguments, shell history, or a saved SQL file. Do
not replace the hash placeholders below with plaintext passwords. The
following is a transaction template, not an instruction to execute during
preparation:

```sql
BEGIN;

-- psql variables are expanded only outside dollar-quoted blocks. These
-- transaction-local settings safely pass normalized values into the block.
SELECT set_config('campushire.utpo_email', lower(trim(:'utpo_email')), TRUE),
       set_config('campushire.dtpo_email', lower(trim(:'dtpo_email')), TRUE);

DO $$
DECLARE
    utpo_email text := current_setting('campushire.utpo_email');
    dtpo_email text := current_setting('campushire.dtpo_email');
BEGIN
    IF (SELECT COUNT(*) FROM roles WHERE role_code IN ('UTPO', 'DTPO')) <> 2 THEN
        RAISE EXCEPTION 'Required UTPO and DTPO roles are not configured';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM departments
        WHERE department_id = 2 AND is_active = TRUE
    ) THEN
        RAISE EXCEPTION 'Department 2 is missing or inactive';
    END IF;
    IF utpo_email IS NULL OR utpo_email = '' OR dtpo_email IS NULL OR dtpo_email = '' THEN
        RAISE EXCEPTION 'Both staff email addresses are required';
    END IF;
    IF utpo_email = dtpo_email THEN
        RAISE EXCEPTION 'The two normalized staff email addresses must be different';
    END IF;
    IF EXISTS (
        SELECT 1 FROM users
        WHERE lower(university_email) IN (utpo_email, dtpo_email)
    ) THEN
        RAISE EXCEPTION 'One or both staff email addresses already exist';
    END IF;
END $$;

INSERT INTO users (
    role_id, department_id, full_name, university_email,
    password_hash, is_active
)
SELECT role_id, NULL, :'utpo_name', current_setting('campushire.utpo_email'),
       :'utpo_bcrypt_hash', TRUE
FROM roles
WHERE role_code = 'UTPO';

INSERT INTO users (
    role_id, department_id, full_name, university_email,
    password_hash, is_active
)
SELECT role_id, 2, :'dtpo_name', current_setting('campushire.dtpo_email'),
       :'dtpo_bcrypt_hash', TRUE
FROM roles
WHERE role_code = 'DTPO';

COMMIT;
```

The inserts use the normalized lowercase values from the transaction-local
settings. The operator must verify the preflight result and email uniqueness
immediately before the inserts. If any check fails, issue `ROLLBACK` and
create no account. If an insert reports a uniqueness or foreign-key error,
fail fast and issue `ROLLBACK`; do not retry with altered role or department
values.

The case-insensitive preflight prevents ordinary repeated runs from creating
case-variant duplicates. The existing database unique constraint remains
case-sensitive and is intentionally preserved. A concurrent provisioning
session could still race between the check and insert, so provisioning must be
performed by one controlled owner session at a time. A future schema-level
unique index on `lower(university_email)` would remove that remaining race, but
is outside this procedure and must not be added here.

No `students` rows are inserted. The U.TPO keeps `department_id = NULL`, while
the D.TPO references department 2, Computer Science & Applications (CMPICA).

## Post-provisioning verification

Without selecting password hashes, verify only identity metadata and counts:

```sql
SELECT u.user_id, u.full_name, u.university_email,
       r.role_code, u.department_id, u.is_active
FROM users u
JOIN roles r ON r.role_id = u.role_id
WHERE lower(u.university_email) IN (lower(:'utpo_email'), lower(:'dtpo_email'));

SELECT COUNT(*)
FROM students s
JOIN users u ON u.user_id = s.user_id
WHERE lower(u.university_email) IN (lower(:'utpo_email'), lower(:'dtpo_email'));
```

The expected result is one active `UTPO` with a null department, one active
`DTPO` with department ID 2, and zero matching `students` rows. Then test login
with the staff client contract (`identifier` plus password) and call `/api/me`.
Do not include tokens or credentials in reports.

## Utility limitations

The CLI is intentionally local-only and manually invoked. It is not a public
API, startup hook, or student-registration path. The application-level
case-insensitive duplicate checks do not eliminate a race between concurrent
provisioning sessions because the existing database unique constraint remains
case-sensitive. Use one controlled owner session at a time; a schema-level
unique index on `lower(university_email)` would require separate approval.
