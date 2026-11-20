# CampusHire project inspection

VERIFIED FROM SOURCE CODE — Flutter student screens and Angular staff routes use a shared Spring Boot API. Java 21, Spring Boot 3.5.6, Angular 22.2 dependency range and Dart ^3.13.2 are declared by the build manifests.

VERIFIED THROUGH RUNTIME TESTING — PostgreSQL 18.6 development catalog, 27 public tables and 241 columns. A fresh Java 21 focused test run completed 58 cases.

INFERRED FROM IMPLEMENTATION — legacy applications/interview_rounds coexist with placement_applications/placement_drive_rounds. The latter back the current recruitment services.

NOT VERIFIED — production deployment, historical cohort eligibility, SMTP delivery, full browser/mobile workflow.

USER INFORMATION REQUIRED — institution, student, guide, project duration, costs, approved screenshots and guide report.

## Modules and workflows

- Identity and recovery (Student or staff). Submit enrollment number or staff email and password. BCrypt verification precedes JWT issuance. Protected requests reload the account and its current role. Recovery verifies a separate email challenge before accepting a new password.
- Registration (Student). Submit registration details, resolve the department and verify the emailed code. Pending registrations are separate from active accounts. Duplicate enrollment or email cannot create a second student.
- Profile and directory (Student). Update personal and academic attributes, skills and photo through the authenticated profile. Directory entries expose a smaller view. Photo bytes reside in file storage, with a key on the student record.
- Companies and documents (DTPO or UTPO). Maintain company contact and recruitment defaults. Select a company when defining a drive, then customize drive fields independently. Attachment metadata identifies the parent and storage key.
- Drive lifecycle (DTPO or UTPO). Create a draft for an active academic year. Validate required fields and deadline before publishing. Department officers manage their assigned scope. Operational archiving retains recruitment history.
- Eligibility and applications (Student). Browse published opportunities in scope. Minimum CGPA, graduation year and exact program-code matching apply where configured. A backlog restriction fails closed because the student record lacks an active backlog count. An accepted offer blocks further applications.
- Recruitment (DTPO or UTPO). Order rounds and evaluate eligible candidates. Application rejection removes an applicant from progression. Previous-round clearance makes a candidate available for a published final round. The officer records the final decision separately.
- Offers and history (Student). View published progress and finalized applications. A selected student may record an offer decision. Acceptance has its own timestamp and leaves the account available for profile and history access.
- Announcements and support (Student and staff). Staff publish notices for the supported audience. Students read announcements, browse People and submit a support complaint through the configured mail service.
- Coordination (UTPO). Select a department and supply the official account details. The server validates the staff email domain and assigns DTPO. The coordinator subsequently signs in on the staff frontend.
- Reporting (DTPO or UTPO). Choose an academic cycle and export CSV or XLSX. The server derives the department restriction from the officer account. UTPO may request university-wide or departmental results.

## PostgreSQL catalog

- academic_years (7 columns)
- announcements (7 columns)
- application_round_results (8 columns)
- applications (7 columns)
- companies (24 columns)
- company_attachments (7 columns)
- department_email_codes (6 columns)
- departments (6 columns)
- drive_eligible_departments (2 columns)
- flyway_schema_history (10 columns)
- interview_rounds (6 columns)
- notifications (9 columns)
- password_change_otps (11 columns)
- pending_student_registrations (12 columns)
- placement_application_exceptions (7 columns)
- placement_applications (8 columns)
- placement_drive_attachments (7 columns)
- placement_drive_programs (2 columns)
- placement_drive_rounds (8 columns)
- placement_drives (35 columns)
- placement_programs (6 columns)
- placement_round_results (6 columns)
- placements (8 columns)
- roles (3 columns)
- student_skills (2 columns)
- students (18 columns)
- users (9 columns)

## HTTP mappings

### HealthController
- `GET /api/health`

### AnnouncementController
- `GET /api/announcements`
- `POST /api/announcements`
- `GET /api/announcements/staff`

### AuthController
- `POST /api/auth/register`
- `POST /api/auth/register/verify`
- `POST /api/auth/register/resend`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/forgot-password/verify-otp`
- `POST /api/auth/forgot-password/reset`

### MeController
- `GET /api/me`
- `PUT /api/me`

### ProfilePhotoController
- `POST /api/me/profile-photo`
- `GET /api/me/profile-photo`
- `DELETE /api/me/profile-photo`

### CompanyAttachmentController
- `GET /api/placement/companies/{companyId}/attachments`
- `POST /api/placement/companies/{companyId}/attachments`
- `GET /api/placement/companies/{companyId}/attachments/{attachmentId}`

### PlacementAttachmentController
- `GET /api/placement/drives/{driveId}/attachments`
- `POST /api/placement/drives/{driveId}/attachments`
- `GET /api/placement/drives/{driveId}/attachments/{attachmentId}`

### PlacementController
- `POST /api/placement/coordinators`
- `GET /api/placement/coordinators`
- `GET /api/placement/departments`
- `GET /api/placement/drives/{driveId}/rounds`
- `POST /api/placement/drives/{driveId}/rounds`
- `PUT /api/placement/drives/{driveId}/rounds/{roundId}`
- `DELETE /api/placement/drives/{driveId}/rounds/{roundId}`
- `GET /api/placement/drives/{driveId}/rounds/{roundId}/candidates`
- `PUT /api/placement/drives/{driveId}/rounds/{roundId}/results/{applicationId}`
- `POST /api/placement/drives/{driveId}/rounds/{roundId}/publish`
- `PUT /api/placement/drives/{driveId}/applications/{applicationId}/outcome`
- `GET /api/placement/student/applications/progress`
- `GET /api/placement/student/applications/history`
- `POST /api/placement/companies`
- `GET /api/placement/companies`
- `PUT /api/placement/companies/{companyId}`
- `POST /api/placement/drives`
- `GET /api/placement/drives`
- `GET /api/placement/round-management/drives`
- `POST /api/placement/drives/{driveId}/publish`
- `POST /api/placement/drives/{driveId}/cancel`
- `POST /api/placement/drives/{driveId}/complete`
- `POST /api/placement/drives/{driveId}/archive`
- `GET /api/placement/reports/academic-years`
- `GET /api/placement/reports/placed.csv`
- `GET /api/placement/reports/placed.xlsx`
- `GET /api/placement/reports/visited-companies.csv`
- `GET /api/placement/reports/visited-companies.xlsx`
- `GET /api/placement/reports/company-hiring.csv`
- `GET /api/placement/reports/company-hiring.xlsx`
- `GET /api/placement/reports/students.csv`
- `GET /api/placement/reports/students.xlsx`
- `GET /api/placement/reports/placement-statistics.csv`
- `GET /api/placement/reports/placement-statistics.xlsx`
- `GET /api/placement/reports/department-performance.csv`
- `GET /api/placement/reports/department-performance.xlsx`
- `GET /api/placement/reports/package-analysis.csv`
- `GET /api/placement/reports/package-analysis.xlsx`
- `GET /api/placement/reports/drive-participation.csv`
- `GET /api/placement/reports/drive-participation.xlsx`
- `GET /api/placement/reports/round-results.csv`
- `GET /api/placement/reports/round-results.xlsx`
- `GET /api/placement/reports/unplaced-students.csv`
- `GET /api/placement/reports/unplaced-students.xlsx`
- `GET /api/placement/student/drives`
- `POST /api/placement/student/drives/{driveId}/applications`
- `POST /api/placement/drives/{driveId}/applications/{applicationId}/reject`
- `GET /api/placement/student/applications`
- `PUT /api/placement/student/applications/{applicationId}/offer`
- `GET /api/placement/applications`
- `GET /api/placement/students`
- `GET /api/placement/dashboard/summary`

### StudentFeatureController
- `GET /api/student/directory`
- `GET /api/student/directory/{studentId}/profile-photo`
- `POST /api/student/support/complaints`
- `POST /api/student/password/otp`
- `POST /api/student/password/verify-otp`
- `PUT /api/student/password`

## Diagram evidence
Role associations follow Angular guards and PlacementAuthorizationService. Activity decisions follow PlacementService and RecruitmentService. Multiplicities follow entity join columns and the PostgreSQL catalog.

## Catalog indexes
- `CREATE UNIQUE INDEX academic_years_pkey ON public.academic_years USING btree (academic_year_id)`
- `CREATE UNIQUE INDEX academic_years_year_label_key ON public.academic_years USING btree (year_label)`
- `CREATE UNIQUE INDEX one_active_academic_year_idx ON public.academic_years USING btree (status) WHERE ((status)::text = 'ACTIVE'::text)`
- `CREATE INDEX announcements_department_idx ON public.announcements USING btree (department_id, published_at DESC, announcement_id DESC)`
- `CREATE UNIQUE INDEX announcements_pkey ON public.announcements USING btree (announcement_id)`
- `CREATE INDEX announcements_published_at_idx ON public.announcements USING btree (published_at DESC, announcement_id DESC)`
- `CREATE UNIQUE INDEX application_round_results_pkey ON public.application_round_results USING btree (result_id)`
- `CREATE UNIQUE INDEX round_results_one_per_application ON public.application_round_results USING btree (application_id, round_id)`
- `CREATE INDEX applications_drive_status_idx ON public.applications USING btree (drive_id, status)`
- `CREATE UNIQUE INDEX applications_one_per_drive ON public.applications USING btree (drive_id, student_id)`
- `CREATE UNIQUE INDEX applications_pkey ON public.applications USING btree (application_id)`
- `CREATE INDEX applications_student_idx ON public.applications USING btree (student_id)`
- `CREATE UNIQUE INDEX companies_company_name_key ON public.companies USING btree (company_name)`
- `CREATE UNIQUE INDEX companies_pkey ON public.companies USING btree (company_id)`
- `CREATE INDEX company_attachments_company_idx ON public.company_attachments USING btree (company_id)`
- `CREATE UNIQUE INDEX company_attachments_pkey ON public.company_attachments USING btree (attachment_id)`
- `CREATE UNIQUE INDEX company_attachments_storage_key_key ON public.company_attachments USING btree (storage_key)`
- `CREATE INDEX department_email_codes_department_idx ON public.department_email_codes USING btree (department_id)`
- `CREATE UNIQUE INDEX department_email_codes_email_code_uq ON public.department_email_codes USING btree (email_code)`
- `CREATE UNIQUE INDEX department_email_codes_pkey ON public.department_email_codes USING btree (department_email_code_id)`
- `CREATE UNIQUE INDEX departments_department_code_key ON public.departments USING btree (department_code)`
- `CREATE UNIQUE INDEX departments_department_name_key ON public.departments USING btree (department_name)`
- `CREATE UNIQUE INDEX departments_pkey ON public.departments USING btree (department_id)`
- `CREATE UNIQUE INDEX drive_eligible_departments_pkey ON public.drive_eligible_departments USING btree (drive_id, department_id)`
- `CREATE UNIQUE INDEX flyway_schema_history_pk ON public.flyway_schema_history USING btree (installed_rank)`
- `CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success)`
- `CREATE UNIQUE INDEX interview_rounds_order_uq ON public.interview_rounds USING btree (drive_id, round_number)`
- `CREATE UNIQUE INDEX interview_rounds_pkey ON public.interview_rounds USING btree (round_id)`
- `CREATE UNIQUE INDEX notifications_pkey ON public.notifications USING btree (notification_id)`
- `CREATE INDEX notifications_recipient_read_idx ON public.notifications USING btree (recipient_user_id, read_at, created_at DESC)`
- `CREATE UNIQUE INDEX password_change_otps_pkey ON public.password_change_otps USING btree (password_change_otp_id)`
- `CREATE INDEX password_change_otps_recovery_token_idx ON public.password_change_otps USING btree (recovery_token_hash) WHERE ((recovery_token_hash IS NOT NULL) AND (used_at IS NULL))`
- `CREATE INDEX password_change_otps_user_idx ON public.password_change_otps USING btree (user_id, requested_at DESC)`
- `CREATE UNIQUE INDEX pending_student_registrations_enrollment_number_key ON public.pending_student_registrations USING btree (enrollment_number)`
- `CREATE UNIQUE INDEX pending_student_registrations_pkey ON public.pending_student_registrations USING btree (pending_registration_id)`
- `CREATE INDEX pending_student_registrations_requested_idx ON public.pending_student_registrations USING btree (requested_at DESC)`
- `CREATE UNIQUE INDEX pending_student_registrations_university_email_key ON public.pending_student_registrations USING btree (university_email)`
- `CREATE UNIQUE INDEX placement_application_exceptions_pkey ON public.placement_application_exceptions USING btree (exception_id)`
- `CREATE UNIQUE INDEX placement_applications_drive_student_uq ON public.placement_applications USING btree (drive_id, student_id)`
- `CREATE UNIQUE INDEX placement_applications_pkey ON public.placement_applications USING btree (application_id)`
- `CREATE INDEX placement_applications_student_idx ON public.placement_applications USING btree (student_id)`
- `CREATE INDEX placement_applications_student_offer_status_idx ON public.placement_applications USING btree (student_id, offer_status)`
- `CREATE INDEX placement_drive_attachments_drive_idx ON public.placement_drive_attachments USING btree (drive_id)`
- `CREATE UNIQUE INDEX placement_drive_attachments_pkey ON public.placement_drive_attachments USING btree (attachment_id)`
- `CREATE UNIQUE INDEX placement_drive_attachments_storage_key_key ON public.placement_drive_attachments USING btree (storage_key)`
- `CREATE UNIQUE INDEX placement_drive_programs_pkey ON public.placement_drive_programs USING btree (drive_id, program_id)`
- `CREATE INDEX placement_drive_programs_program_idx ON public.placement_drive_programs USING btree (program_id)`
- `CREATE INDEX placement_drive_rounds_drive_idx ON public.placement_drive_rounds USING btree (drive_id, round_order)`
- `CREATE UNIQUE INDEX placement_drive_rounds_drive_order_uq ON public.placement_drive_rounds USING btree (drive_id, round_order)`
- `CREATE UNIQUE INDEX placement_drive_rounds_pkey ON public.placement_drive_rounds USING btree (round_id)`
- `CREATE INDEX drives_year_status_idx ON public.placement_drives USING btree (academic_year_id, status)`
- `CREATE INDEX placement_drives_department_idx ON public.placement_drives USING btree (department_id)`
- `CREATE INDEX placement_drives_operational_archive_idx ON public.placement_drives USING btree (operationally_archived, status)`
- `CREATE UNIQUE INDEX placement_drives_pkey ON public.placement_drives USING btree (drive_id)`
- `CREATE INDEX placement_drives_status_deadline_idx ON public.placement_drives USING btree (status, application_closes_at)`
- `CREATE UNIQUE INDEX placement_programs_pkey ON public.placement_programs USING btree (program_id)`
- `CREATE UNIQUE INDEX placement_programs_program_code_key ON public.placement_programs USING btree (program_code)`
- `CREATE INDEX placement_round_results_application_idx ON public.placement_round_results USING btree (application_id)`
- `CREATE UNIQUE INDEX placement_round_results_pkey ON public.placement_round_results USING btree (round_result_id)`
- `CREATE UNIQUE INDEX placement_round_results_round_application_uq ON public.placement_round_results USING btree (round_id, application_id)`
- `CREATE UNIQUE INDEX placements_application_id_key ON public.placements USING btree (application_id)`
- `CREATE UNIQUE INDEX placements_pkey ON public.placements USING btree (placement_id)`
- `CREATE UNIQUE INDEX roles_pkey ON public.roles USING btree (role_id)`
- `CREATE UNIQUE INDEX roles_role_code_key ON public.roles USING btree (role_code)`
- `CREATE UNIQUE INDEX student_skills_pk ON public.student_skills USING btree (student_id, skill_name)`
- `CREATE INDEX student_skills_skill_idx ON public.student_skills USING btree (skill_name)`
- `CREATE INDEX students_department_idx ON public.students USING btree (department_id)`
- `CREATE UNIQUE INDEX students_enrollment_number_key ON public.students USING btree (enrollment_number)`
- `CREATE UNIQUE INDEX students_pkey ON public.students USING btree (student_id)`
- `CREATE UNIQUE INDEX students_user_id_key ON public.students USING btree (user_id)`
- `CREATE UNIQUE INDEX users_email_uq ON public.users USING btree (university_email)`
- `CREATE UNIQUE INDEX users_pkey ON public.users USING btree (user_id)`

## Source evidence
- Placed students — final SELECTED application, student, company, CTC and recorded selection date.
- Visited companies — drive-level company details, status, deadline, annual CTC and stipend. Department drives can appear without applicants.
- Registered students — current roster, enrollment, program and academic profile. The selected cycle cannot establish registration year.
- Company-wise hiring — grouped selected records by company, department, drive and placement date. Overall totals sum groups and can repeat an individual hired across drives.
- Placement statistics — distinct selected students, companies, drives and package range. The denominator labelled eligible is the current scoped roster.
- Department performance — selected students divided by current department roster. Historical membership and eligibility are not snapshotted.
- Package analysis — drive CTC and distinct selected students per drive with selections. Stipend is separate.
- Drive participation — applicant, selected and rejection counts by drive with applications. Pending applications are distinct from rejected records.
- Round-wise results — stored pass, fail and pending rows grouped by round. Candidates without a stored result row may be absent.
- Unplaced students — current scoped students without a selected application in the chosen cycle. This describes recorded outcomes only.

The current dashboard and Flutter client share REST identity but use separate navigation. The code paths below anchor the module inventory.

### Backend services
- `backend/src/main/java/com/placementmanagement/api/announcement/service/AnnouncementService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/security/JwtService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/AuthService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/CurrentUserService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/ForgotPasswordService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/ProfilePhotoService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/ProfilePhotoStorageService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/RegistrationOtpAttemptService.java`
- `backend/src/main/java/com/placementmanagement/api/auth/service/RegistrationService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/CompanyAttachmentService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/CoordinatorService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/PlacementAttachmentService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/PlacementAttachmentStorageService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/PlacementAuthorizationService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/PlacementReportService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/PlacementService.java`
- `backend/src/main/java/com/placementmanagement/api/placement/service/RecruitmentService.java`
- `backend/src/main/java/com/placementmanagement/api/student/service/PasswordChangeTransactionService.java`
- `backend/src/main/java/com/placementmanagement/api/student/service/PasswordOtpAttemptService.java`
- `backend/src/main/java/com/placementmanagement/api/student/service/PasswordOtpIssuanceService.java`
- `backend/src/main/java/com/placementmanagement/api/student/service/StudentFeatureService.java`
- `backend/src/main/java/com/placementmanagement/tools/StaffProvisioningService.java`

### Angular components
- `staff-frontend/src/app/features/dtpo/dashboard/dtpo-dashboard.component.ts`
- `staff-frontend/src/app/features/login/login.component.ts`
- `staff-frontend/src/app/features/shared/staff-dashboard/staff-dashboard.component.ts`
- `staff-frontend/src/app/features/utpo/dashboard/utpo-dashboard.component.ts`

### Flutter screens
- `flutter_app/lib/screens/announcements_page.dart`
- `flutter_app/lib/screens/auth_page.dart`
- `flutter_app/lib/screens/change_password_page.dart`
- `flutter_app/lib/screens/forgot_password_page.dart`
- `flutter_app/lib/screens/more_page.dart`
- `flutter_app/lib/screens/people_page.dart`
- `flutter_app/lib/screens/profile_page.dart`
- `flutter_app/lib/screens/registration_otp_page.dart`
- `flutter_app/lib/screens/settings_page.dart`
- `flutter_app/lib/screens/student_home.dart`
- `flutter_app/lib/screens/support_page.dart`
- `flutter_app/lib/screens/welcome_screen.dart`

### SQL changes
- `backend/src/main/resources/db/migration/V10__add_company_defaults_and_attachments.sql`
- `backend/src/main/resources/db/migration/V11__add_simple_stipend_fields.sql`
- `backend/src/main/resources/db/migration/V12__add_drive_operational_archive.sql`
- `backend/src/main/resources/db/migration/V13__add_application_placement_selected_at.sql`
- `backend/src/main/resources/db/migration/V14__create_announcements.sql`
- `backend/src/main/resources/db/migration/V15__create_password_change_otps.sql`
- `backend/src/main/resources/db/migration/V16__create_pending_student_registrations.sql`
- `backend/src/main/resources/db/migration/V17__add_password_recovery_otp_purpose.sql`
- `backend/src/main/resources/db/migration/V2__create_department_email_codes.sql`
- `backend/src/main/resources/db/migration/V3__add_student_profile_photo_key.sql`
- `backend/src/main/resources/db/migration/V4__create_placement_domain.sql`
- `backend/src/main/resources/db/migration/V5__create_recruitment_rounds.sql`
- `backend/src/main/resources/db/migration/V6__add_student_skills_and_program_duration.sql`
- `backend/src/main/resources/db/migration/V7__add_student_offer_decisions.sql`
- `backend/src/main/resources/db/migration/V8__add_application_rejected_status.sql`
- `backend/src/main/resources/db/migration/V9__add_drive_details_and_attachments.sql`
