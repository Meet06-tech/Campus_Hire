# CampusHire Activity Diagram Notes

## Scope

This activity diagram represents the current implemented placement workflow across the Flutter student app, Angular staff dashboard, Spring Boot backend, and PostgreSQL persistence layer.

## Swimlanes

- **Student**: logs in with enrollment credentials, browses published drives, selects a drive, applies, and views application or recruitment-round progress.
- **Angular Staff Dashboard / DTPO**: creates companies and draft drives, publishes department drives, configures ordered rounds, evaluates candidates, publishes round results, and records final outcomes.
- **UTPO**: has the existing university-wide staff scope and coordinator-management capability. The diagram separates UTPO actions where they differ from DTPO actions.
- **Spring Boot Backend**: authenticates and reloads the current account, applies role and department authorization, checks drive visibility and eligibility, protects duplicate applications, and controls round progression.
- **PostgreSQL**: stores users, students, companies, academic years, drives, applications, recruitment rounds, and round results.

## Decisions represented

1. Invalid credentials stop the flow.
2. Student drive visibility and application require published status, an open deadline, department scope, program matching, CGPA and the configured backlog rule.
3. A unique student-drive application is enforced before persisting `APPLIED`.
4. Staff requests are authorized from the authenticated role and department. DTPO scope is department-bound; UTPO scope is university-wide.
5. Publishing validates the company, active academic year, required drive fields, status and deadline before changing `DRAFT` to `PUBLISHED`.
6. Later-round candidates must have passed the immediately previous round. Withdrawn applications are excluded from candidate lists.
7. Round results are published before students can see them. A final `PASSED` result is required before the staff member can record `SELECTED` or `REJECTED`.

## Implementation gaps and caveats

- The recruitment-round Java service, Angular DTPO round controls, V5 migration, and Flutter progress model are present in the current working tree. The V5 database rollout still needs controlled deployment and validation before it can be treated as live in every environment.
- The current `PlacementService` fails closed when `maximumActiveBacklogs` is configured because the existing `students` model has no active-backlog field. The diagram therefore shows the backlog check as a backend eligibility decision, without claiming that a backlog value is currently available.
- The diagram does not show every REST endpoint or every UI screen. It focuses on the implemented business workflow and the decisions that change its outcome.
- No behavior was inferred from names alone. The flow was checked against `PlacementController`, `PlacementService`, `RecruitmentService`, `PlacementAuthorizationService`, the placement entities and repositories, Flutter placement services/screens, Angular dashboard actions, and migration V5.
