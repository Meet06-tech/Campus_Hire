# CampusHire — Data Dictionary

**Verification date:** 2026-10-27  
**Database:** `placement_management`  
**Verified schema:** `public`  
**Live verification:** Yes. The catalog was inspected using the configured local database connection as the database owner. No passwords, hashes, tokens, or row data were queried or included.

## Scope and conventions

- This dictionary describes every user table returned by PostgreSQL catalog inspection. System schemas and system tables are excluded.
- `bigint GENERATED ALWAYS AS IDENTITY` means PostgreSQL generates the key from the table's identity sequence.
- Descriptions are based on column names, constraints, migrations, and mapped Spring entities. Where a table is not mapped by the current backend entities, that is marked as **inferred / legacy schema**.
- Nullable means the database column permits `NULL`; it does not necessarily mean the application accepts every null value.
- Sensitive fields are described without showing any stored values.

## Table summary

| Table | Purpose | Relationships |
|---|---|---|
| `academic_years` | Academic-year master data used by placement drives. | Referenced by `placement_drives`. |
| `application_round_results` | Round-result records for the parallel legacy application/interview-round model. **Inferred / legacy schema.** | References `applications`, `interview_rounds`, and `users`. |
| `applications` | Legacy/parallel student application model. **Inferred / legacy schema.** | References `placement_drives` and `students`; referenced by `application_round_results`, `notifications`, and `placements`. |
| `companies` | Company master and recruiter contact information. | Referenced by `placement_drives` and `placements`. |
| `department_email_codes` | Configurable email-local-part codes used to resolve student departments. | References `departments`. |
| `departments` | Department master data and active status. | Referenced by users, students, drives, email codes, and the legacy drive-department junction. |
| `drive_eligible_departments` | Legacy junction for drive-to-department eligibility. **Inferred / legacy schema.** | Links `placement_drives` and `departments`. |
| `interview_rounds` | Legacy interview-round model. **Inferred / legacy schema.** | References `placement_drives`; referenced by `application_round_results`. |
| `notifications` | User notifications optionally related to a drive or application. **Inferred / legacy schema.** | References `users`, `placement_drives`, and `applications`. |
| `placement_application_exceptions` | Department/application eligibility exceptions granted for individual students. **Inferred / schema support.** | References `students` and granting `users`. |
| `placement_applications` | Current V1 student applications to placement drives. | References `placement_drives` and `students`; referenced by `placement_round_results`. |
| `placement_drive_programs` | Junction of drives and eligible programs. Empty set means no program restriction in current service logic. | Links `placement_drives` and `placement_programs`; drive link cascades on drive deletion. |
| `placement_drive_rounds` | Current recruitment rounds for a drive. | References `placement_drives`; referenced by `placement_round_results`. |
| `placement_drives` | Placement-drive master record, eligibility, dates, status, and scope. | References academic year, company, creator user, and optional department; referenced by applications, rounds, programs, notifications, and legacy tables. |
| `placement_programs` | Program/branch master used for drive eligibility. | Referenced by `placement_drive_programs`. |
| `placement_round_results` | Current recruitment-round results for applications. | References `placement_drive_rounds`, `placement_applications`, and optionally recording user. |
| `placements` | Final placement records. **Inferred / legacy or future schema support.** | References `applications`, `students`, `companies`, and confirming user. |
| `roles` | Authentication role master. | Referenced by `users`; role code is restricted to `UTPO`, `DTPO`, and `STUDENT`. |
| `students` | Student profile and academic/placement attributes. | References `users` and `departments`; referenced by applications, exceptions, and placements. |
| `users` | Authentication accounts, role, optional department, and password hash. | References `roles` and optional `departments`; referenced throughout staff/audit relationships. |

## Relationships

### Authentication and academic structure

- `users.role_id → roles.role_id` is mandatory.
- `users.department_id → departments.department_id` is optional, allowing university-wide users such as UTPO.
- `students.user_id → users.user_id` is mandatory and unique, so one user has at most one student profile.
- `students.department_id → departments.department_id` is mandatory.
- `department_email_codes.department_id → departments.department_id` is mandatory.

### Drives and eligibility

- `placement_drives.academic_year_id → academic_years.academic_year_id` and `placement_drives.company_id → companies.company_id` are mandatory.
- `placement_drives.created_by_user_id → users.user_id` is mandatory.
- `placement_drives.department_id → departments.department_id` is nullable: `NULL` represents a university-wide drive in the current application model.
- `placement_drive_programs` is a many-to-many junction between drives and programs. Its composite primary key prevents duplicate program assignments.
- `drive_eligible_departments` is a separate many-to-many junction present in the database; it belongs to the parallel/legacy schema model and is not the current Java entity used for V1 program eligibility.

### Applications and recruitment

- `placement_applications` links one student to one drive and has a unique `(drive_id, student_id)` pair.
- `placement_drive_rounds` stores ordered rounds per drive; `(drive_id, round_order)` is unique.
- `placement_round_results` links one application to one round; `(round_id, application_id)` is unique.
- `application_round_results` performs a similar function for the separate legacy `applications`/`interview_rounds` model.

## Individual table dictionaries

### 1. `academic_years`

Purpose: academic-year lookup and lifecycle status for drives.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `academic_year_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Database-generated academic-year identifier. |
| `year_label` | `character varying` | No | Unique | None | Display label such as an academic-year range. |
| `starts_on` | `date` | No | — | None | Academic-year start date. |
| `ends_on` | `date` | No | — | None | Academic-year end date; check requires it to be after `starts_on`. |
| `status` | `character varying` | No | — | `'PLANNED'` | Lifecycle status; check permits `PLANNED`, `ACTIVE`, or `CLOSED`. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK `academic_years_pkey`; unique `year_label`; check `ends_on > starts_on`; status check; partial unique index `one_active_academic_year_idx` permits at most one `ACTIVE` row.

### 2. `application_round_results`

Purpose: parallel legacy round-result storage for `applications` and `interview_rounds` (**inferred / not mapped by current Java entities**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `result_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Result identifier. |
| `application_id` | `bigint` | No | FK → `applications.application_id` | None | Legacy application receiving the result. |
| `round_id` | `bigint` | No | FK → `interview_rounds.round_id` | None | Legacy interview round. |
| `entered_by_user_id` | `bigint` | No | FK → `users.user_id` | None | User who entered the result. |
| `result_status` | `character varying` | No | — | `'PENDING'` | Result state; check permits `PENDING`, `PASSED`, `FAILED`, or `NOT_ATTENDED`. |
| `result_note` | `text` | Yes | — | None | Optional result note. |
| `result_recorded_at` | `timestamp with time zone` | Yes | — | None | Time the result was recorded. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK; FKs to the legacy application, interview round, and user tables; unique `(application_id, round_id)`; result-status check.

### 3. `applications`

Purpose: parallel legacy application table (**inferred / not mapped by current Java entities**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `application_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Legacy application identifier. |
| `drive_id` | `bigint` | No | FK → `placement_drives.drive_id` | None | Placement drive applied to. |
| `student_id` | `bigint` | No | FK → `students.student_id` | None | Applicant student. |
| `submitted_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Submission timestamp. |
| `status` | `character varying` | No | — | `'SUBMITTED'` | Legacy lifecycle status; check permits `SUBMITTED`, `SHORTLISTED`, `REJECTED`, `SELECTED`, or `PLACED`. |
| `eligibility_checked` | `boolean` | No | — | `false` | Whether eligibility was checked for the application. |
| `eligibility_note` | `text` | Yes | — | None | Optional eligibility explanation. |

Constraints and indexes: PK; FKs to drives and students; unique `(drive_id, student_id)`; index `(drive_id, status)`; index on `student_id`; status check.

### 4. `companies`

Purpose: company master and recruiter/contact details.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `company_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Company identifier. |
| `company_name` | `character varying` | No | Unique | None | Company name; non-blank check. |
| `profile` | `text` | Yes | — | None | Company profile/description. |
| `contact_name` | `character varying` | Yes | — | None | HR/recruiter contact name. |
| `contact_email` | `character varying` | Yes | — | None | HR/recruiter email. |
| `contact_phone` | `character varying` | Yes | — | None | HR/recruiter phone. |
| `website_url` | `character varying` | Yes | — | None | Company website. |
| `industry` | `character varying` | Yes | — | None | Industry classification. |
| `is_active` | `boolean` | No | — | `true` | Whether the company is active. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK; unique company name; non-blank company-name check.

### 5. `department_email_codes`

Purpose: configurable uppercase codes extracted from university email addresses and mapped to departments.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `department_email_code_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Mapping identifier. |
| `department_id` | `bigint` | No | FK → `departments.department_id` | None | Department resolved from the code. |
| `email_code` | `character varying` | No | Unique | None | Uppercase alphanumeric code; check enforces uppercase and `^[A-Z0-9]+$`. |
| `is_active` | `boolean` | No | — | `true` | Whether the mapping may be used for registration. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK; FK to departments; globally unique code; code-format check; index on `department_id`.

### 6. `departments`

Purpose: department master and active/inactive state.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `department_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Department identifier. |
| `department_code` | `character varying` | No | Unique | None | Short department code. |
| `department_name` | `character varying` | No | Unique | None | Department display name. |
| `is_active` | `boolean` | No | — | `true` | Whether the department is active. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK; unique department code; unique department name.

### 7. `drive_eligible_departments`

Purpose: legacy many-to-many drive/departments eligibility junction (**inferred / not mapped by current Java entities**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `drive_id` | `bigint` | No | PK part; FK → `placement_drives.drive_id` | None | Drive in the legacy department-eligibility relation. |
| `department_id` | `bigint` | No | PK part; FK → `departments.department_id` | None | Department eligible for the drive. |

Constraints and indexes: composite PK `(drive_id, department_id)`; FKs to drives and departments.

### 8. `interview_rounds`

Purpose: legacy interview-round model (**inferred / not mapped by current Java entities**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `round_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Legacy round identifier. |
| `drive_id` | `bigint` | No | FK → `placement_drives.drive_id` | None | Drive owning the round. |
| `round_number` | `integer` | No | Unique part with drive | None | Positive round order. |
| `round_name` | `character varying` | No | — | None | Round display name. |
| `scheduled_at` | `timestamp with time zone` | Yes | — | None | Optional scheduled time. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |

Constraints and indexes: PK; FK to drives; unique `(drive_id, round_number)`; positive-order check.

### 9. `notifications`

Purpose: notification records for users, optionally tied to a drive or application (**inferred / schema support**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `notification_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Notification identifier. |
| `recipient_user_id` | `bigint` | No | FK → `users.user_id` | None | User receiving the notification. |
| `notification_type` | `character varying` | No | — | None | Notification category; check permits `DRIVE_ANNOUNCEMENT`, `APPLICATION_CONFIRMATION`, or `SELECTION_UPDATE`. |
| `title` | `character varying` | No | — | None | Notification title. |
| `body` | `text` | No | — | None | Notification body. |
| `related_drive_id` | `bigint` | Yes | FK → `placement_drives.drive_id` | None | Optional related drive. |
| `related_application_id` | `bigint` | Yes | FK → `applications.application_id` | None | Optional related legacy application. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `read_at` | `timestamp with time zone` | Yes | — | None | Time the notification was read. |

Constraints and indexes: PK; FKs to users, drives, and legacy applications; notification-type check; index `(recipient_user_id, read_at, created_at DESC)`.

### 10. `placement_application_exceptions`

Purpose: individual student exceptions to normal application eligibility (**inferred / schema support; no current Java entity was found**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `exception_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Exception identifier. |
| `student_id` | `bigint` | No | FK → `students.student_id` | None | Student receiving the exception. |
| `granted_by_user_id` | `bigint` | No | FK → `users.user_id` | None | User granting the exception. |
| `valid_from` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Exception start time. |
| `valid_until` | `timestamp with time zone` | Yes | — | None | Optional expiry; if present must be after `valid_from`. |
| `reason` | `text` | No | — | None | Justification for the exception. |
| `is_active` | `boolean` | No | — | `true` | Whether the exception is active. |

Constraints and indexes: PK; FKs to students and users; date-range check. No additional unique/index definitions were present in the live catalog.

### 11. `placement_applications`

Purpose: current V1 student applications to placement drives.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `application_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Application identifier. |
| `drive_id` | `bigint` | No | FK → `placement_drives.drive_id` | None | Applied placement drive. |
| `student_id` | `bigint` | No | FK → `students.student_id` | None | Applicant student. |
| `applied_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Application timestamp. |
| `status` | `character varying` | No | — | `'APPLIED'` | Current status; check permits `APPLIED`, `SHORTLISTED`, `REJECTED`, `SELECTED`, or `WITHDRAWN`. |

Constraints and indexes: PK; FKs to drives and students; unique `(drive_id, student_id)` prevents duplicate applications; index on `student_id`.

### 12. `placement_drive_programs`

Purpose: normalized drive-to-program eligibility junction.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `drive_id` | `bigint` | No | PK part; FK → `placement_drives.drive_id` | None | Drive being restricted to a program. |
| `program_id` | `bigint` | No | PK part; FK → `placement_programs.program_id` | None | Eligible program. |

Constraints and indexes: composite PK `(drive_id, program_id)`; drive FK uses `ON DELETE CASCADE`; program FK; index on `program_id`. In current service logic, an empty set of rows for a drive means no program restriction.

### 13. `placement_drive_rounds`

Purpose: current recruitment-round configuration and publication state.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `round_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Round identifier. |
| `drive_id` | `bigint` | No | FK → `placement_drives.drive_id` | None | Owning placement drive. |
| `round_order` | `integer` | No | Unique part with drive | None | Positive order of the round. |
| `round_name` | `character varying` | No | — | None | Round name; non-blank check. |
| `is_final` | `boolean` | No | — | `false` | Whether this is the final round. |
| `results_published` | `boolean` | No | — | `false` | Whether round results are published. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK; drive FK with `ON DELETE CASCADE`; unique `(drive_id, round_order)`; positive-order and non-blank-name checks; indexes `(drive_id, round_order)` and its unique equivalent.

### 14. `placement_drives`

Purpose: current placement-drive record, scope, eligibility, dates, and lifecycle status.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `drive_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Drive identifier. |
| `academic_year_id` | `bigint` | No | FK → `academic_years.academic_year_id` | None | Academic year for the drive. |
| `company_id` | `bigint` | No | FK → `companies.company_id` | None | Recruiting company. |
| `created_by_user_id` | `bigint` | No | FK → `users.user_id` | None | Staff account that created the drive. |
| `job_title` | `character varying` | No | — | None | Job/position title. |
| `job_description` | `text` | Yes | — | None | Job description. |
| `eligibility_criteria` | `text` | Yes | — | None | Free-form legacy eligibility description. |
| `compensation_package` | `character varying` | Yes | — | None | Package/salary information. |
| `location` | `character varying` | Yes | — | None | Job location. |
| `work_mode` | `character varying` | Yes | — | None | Work mode; check permits `ONSITE`, `HYBRID`, or `REMOTE` when present. |
| `application_opens_at` | `timestamp with time zone` | No | — | None | Application opening time. |
| `application_closes_at` | `timestamp with time zone` | No | — | None | Application closing/deadline time; must be after opening. |
| `scheduled_at` | `timestamp with time zone` | Yes | — | None | Optional scheduled drive time. |
| `instructions` | `text` | Yes | — | None | Additional instructions. |
| `status` | `character varying` | No | — | `'DRAFT'` | Lifecycle status; check permits `DRAFT`, `PUBLISHED`, `CLOSED`, `CANCELLED`, or `ARCHIVED`. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |
| `department_id` | `bigint` | Yes | FK → `departments.department_id` | None | Optional department scope; `NULL` is the current university-wide representation. |
| `minimum_cgpa` | `numeric` | Yes | — | None | Minimum CGPA; check permits null or the range 0–10. |
| `graduation_year` | `smallint` | Yes | — | None | Optional required graduation year. |
| `maximum_active_backlogs` | `integer` | Yes | — | None | Maximum allowed active backlogs; check requires non-negative when present. |

Constraints and indexes: PK; FKs to academic years, companies, creator user, and optional department; status, work-mode, application-window, CGPA, and backlog checks; indexes on department, `(academic_year_id, status)`, and `(status, application_closes_at)`.

### 15. `placement_programs`

Purpose: program/branch reference data used by drive eligibility.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `program_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Program identifier. |
| `program_code` | `character varying` | No | Unique | None | Program/branch code. |
| `program_name` | `character varying` | No | — | None | Program display name. |
| `is_active` | `boolean` | No | — | `true` | Whether the program can be selected for new drives. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |

Constraints and indexes: PK; unique program code.

### 16. `placement_round_results`

Purpose: current recruitment-round result for a placement application.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `round_result_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Round-result identifier. |
| `round_id` | `bigint` | No | FK → `placement_drive_rounds.round_id` | None | Current recruitment round. |
| `application_id` | `bigint` | No | FK → `placement_applications.application_id` | None | Current V1 application. |
| `result` | `character varying` | No | — | `'PENDING'` | Result; check permits `PENDING`, `PASSED`, or `FAILED`. |
| `recorded_at` | `timestamp with time zone` | Yes | — | None | Time the result was recorded. |
| `recorded_by_user_id` | `bigint` | Yes | FK → `users.user_id` | None | Staff user who recorded it. |

Constraints and indexes: PK; FKs to rounds and applications use `ON DELETE CASCADE`, optional FK to recording user; unique `(round_id, application_id)`; index on `application_id`; result check.

### 17. `placements`

Purpose: final placement records (**inferred / legacy or future schema support; not mapped by current Java entities**).

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `placement_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Placement identifier. |
| `application_id` | `bigint` | No | Unique; FK → `applications.application_id` | None | Legacy application resulting in placement. |
| `student_id` | `bigint` | No | FK → `students.student_id` | None | Placed student. |
| `company_id` | `bigint` | No | FK → `companies.company_id` | None | Hiring company. |
| `placed_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Placement confirmation time. |
| `package_details` | `character varying` | Yes | — | None | Package/offer details. |
| `notes` | `text` | Yes | — | None | Additional placement notes. |
| `confirmed_by_user_id` | `bigint` | No | FK → `users.user_id` | None | User confirming the placement. |

Constraints and indexes: PK; unique application ID; FKs to legacy application, student, company, and confirming user.

### 18. `roles`

Purpose: authentication role master.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `role_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Role identifier. |
| `role_code` | `character varying` | No | Unique | None | Persisted role code; check restricts values to `UTPO`, `DTPO`, or `STUDENT`. |
| `role_name` | `character varying` | No | — | None | Human-readable role name. |

Constraints and indexes: PK; unique role code; role-code check.

### 19. `students`

Purpose: student profile, academic information, placement flags, and profile-photo reference.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `student_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Student profile identifier. |
| `user_id` | `bigint` | No | Unique; FK → `users.user_id` | None | Owning authentication account. |
| `enrollment_number` | `character varying` | No | Unique | None | University enrollment identifier. |
| `department_id` | `bigint` | No | FK → `departments.department_id` | None | Student department. |
| `admission_year` | `smallint` | Yes | — | None | Admission year; check permits 1900–2200 when present. |
| `graduation_year` | `smallint` | Yes | — | None | Graduation year; check permits 1900–2200 when present. |
| `date_of_birth` | `date` | Yes | — | None | Date of birth. |
| `phone_number` | `character varying` | Yes | — | None | Phone number. |
| `academic_program` | `character varying` | Yes | — | None | Student program/branch value used by current V1 program matching. |
| `current_semester` | `smallint` | Yes | — | None | Current semester; positive when present. |
| `cgpa` | `numeric` | Yes | — | None | CGPA; check permits null or 0–10. |
| `career_profile` | `text` | Yes | — | None | Career/profile content. |
| `placement_registration` | `boolean` | No | — | `false` | Whether the student registered for placement. |
| `contract_signed` | `boolean` | No | — | `false` | Whether the placement contract is signed. |
| `account_status` | `character varying` | No | — | `'ACTIVE'` | Student status; check permits `ACTIVE`, `GRADUATED`, or `DEACTIVATED`. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |
| `profile_photo_key` | `character varying` | Yes | — | None | Storage key for the profile photo; binary photo data is not stored here. |

Constraints and indexes: PK; unique user ID and enrollment number; FKs to users and departments; CGPA, semester, year, account-status, and boolean not-null constraints; indexes on department and enrollment/user unique keys.

### 20. `users`

Purpose: authentication account, role assignment, optional staff department, and BCrypt password hash.

| Column Name | Data Type | Nullable | Key (PK/FK/Unique) | Default Value | Description |
|---|---|---:|---|---|---|
| `user_id` | `bigint GENERATED ALWAYS AS IDENTITY` | No | PK | Identity sequence | Authentication account identifier. |
| `role_id` | `bigint` | No | FK → `roles.role_id` | None | Persisted role. |
| `department_id` | `bigint` | Yes | FK → `departments.department_id` | None | Optional account department; UTPO may be university-wide with null. |
| `full_name` | `character varying` | No | — | None | Account holder name; non-blank check. |
| `university_email` | `character varying` | No | Unique | None | University email; must contain an `@` after at least one character. |
| `password_hash` | `character varying` | No | Sensitive | None | BCrypt password hash. Values are intentionally not inspected or documented. |
| `is_active` | `boolean` | No | — | `true` | Account-active flag used by authentication. |
| `created_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Creation timestamp. |
| `updated_at` | `timestamp with time zone` | No | — | `CURRENT_TIMESTAMP` | Last-update timestamp. |

Constraints and indexes: PK; FKs to roles and optional departments; unique university email; non-blank name and basic email-shape checks; identity sequence. The database owner for this table and the other inspected public tables is `meetbhoi`.

## Verification checklist

- Live catalog table list: **20 of 20 user tables documented above**.
- Every live column, nullability, identity/default expression, primary key, foreign key, unique constraint, check constraint, and user-table index was inspected.
- No inserts, updates, deletes, migrations, permission changes, or schema changes were performed.
- Password hashes and other secret values were not selected.
