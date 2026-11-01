# CampusHire Project Review Notes

## Slide 1 — Title

Introduce CampusHire as the Placement Management System for CHARUSAT University and CMPICA. State that the presentation describes the repository as it exists today, including the recruitment-round milestone and verification limits.

## Slide 2 — Placement coordination in one workspace

Explain that the platform gives students and placement staff one shared workflow. Students use Flutter; staff use Angular; both rely on the Spring Boot API.

## Slide 3 — The placement problem

Describe the practical problems: fragmented records, uncertain eligibility, department boundaries and slow status updates. Avoid claiming measured improvements.

## Slide 4 — Project objectives

Walk through secure identity, structured placement data, department-aware authorization and transparent recruitment progress.

## Slide 5 — Existing workflow and proposed platform

Use the comparison to explain why a centralized placement system helps. Emphasize that the platform makes rules explicit in the backend.

## Slide 6 — Roles and access boundaries

Explain the three persisted roles. Students manage only their own activity. DTPOs work within their assigned department. UTPOs have university-wide scope.

## Slide 7 — Technology stack

Mention Java 21 and Spring Boot 3.5.6 on the backend, PostgreSQL for persistence, Flutter for students, Angular for staff, JWT for sessions and BCrypt for passwords.

## Slide 8 — System architecture

Describe the request flow: a client sends a JWT-authenticated request, Spring Boot applies business rules and JPA repositories persist or read PostgreSQL data.

## Slide 9 — Functional modules

Point out that the repository contains authentication, student profiles, companies, placement drives, applications, dashboards and the new recruitment-round service.

## Slide 10 — Database design

Explain the important relationships: users connect to departments and students; drives connect companies, academic years, departments and programs; applications connect students to drives; rounds and results extend applications.

## Slide 11 — Authentication and user management

Students log in with enrollment number. Staff log in with the identifier/email contract. BCrypt verifies passwords and JWT protects subsequent requests. Staff accounts do not need a Student row.

## Slide 12 — Placement drive management

Describe the lifecycle from company record to draft, publication, student visibility and closing. Eligibility uses department, program, graduation year, CGPA and configured backlog rules.

## Slide 13 — Student application workflow

Show how a student browses published eligible drives, inspects the details, applies and later tracks the application. The unique database constraint prevents duplicate applications.

## Slide 14 — Recruitment round management

Explain configurable ordered rounds. Candidates start Pending, only Passed candidates advance, staff publish results separately, and a final Selected or Not Selected outcome is recorded only after the final round is passed.

## Slide 15 — UTPO and DTPO dashboards

Explain the difference between university-wide UTPO access and department-scoped DTPO access. Mention that the Angular source includes round-management controls and the backend owns the authorization.

## Slide 16 — Testing and verification

Be precise. Backend compilation and test compilation passed. Flutter widget tests passed. Flutter analysis reported only three style infos. Angular type-check passed. The full backend suite remains blocked by Mockito/Byte Buddy agent attachment in the local environment, and the Angular production bundle was terminated by the local Node process.

## Slide 17 — Current limitations and next steps

The V5 migration must be applied and verified in PostgreSQL. Tests should run in an environment that supports the Mockito agent. Academic-year, department and program data must be configured before realistic placement tests.

## Slide 18 — Conclusion and Q&A

Close with the value of a shared, secure and traceable placement workflow. Invite questions about the authorization model, database relationships and recruitment-round design.

## Architecture in simple terms

Flutter and Angular are client applications. They call Spring Boot REST endpoints. Spring Security validates JWTs, services enforce role and department rules, JPA maps Java entities to PostgreSQL tables, and migrations define additive database changes.

## Terms to remember

- **JWT:** a signed token used to carry an authenticated session context.
- **BCrypt:** a one-way password hashing algorithm used for stored passwords.
- **DTO:** a request or response object that defines an API contract without serializing entities directly.
- **DTPO:** department-level placement coordinator.
- **UTPO:** university-level placement coordinator.
- **Eligibility:** backend checks that determine whether a student can see or apply to a drive.
- **Migration:** a versioned SQL change used to bring the database schema forward.

## Implemented versus pending

### Implemented in source

- Student registration and login.
- Staff login for UTPO and DTPO accounts.
- JWT authentication and `/api/me`.
- Student profile and profile-photo flows.
- Company and placement-drive APIs.
- Drive publication and student application protection.
- Angular staff dashboards and Flutter student placement screens.
- Recruitment-round entities, endpoints and client-side progress views in the current branch.

### Pending or limited verification

- V5 database migration has not been claimed as applied in this presentation.
- Full backend test execution is blocked by local Mockito/Byte Buddy agent attachment.
- Angular production build needs a stable local Node runtime because the current process exits with code 134.
- Live end-to-end round management still requires controlled database rollout and verification.
- The repository README still contains older V1 wording and should be refreshed separately.
