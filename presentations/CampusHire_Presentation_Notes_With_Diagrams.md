# CampusHire — Presentation Notes

## 1. Title
CampusHire is the unified placement-management platform for students, department TPOs and the university TPO. The deck describes the implemented repository state and the next rollout considerations.

## 2. Introduction
The platform connects student discovery and applications with staff coordination, while keeping authorization and eligibility decisions in the Spring Boot backend.

## 3. The placement problem
The system addresses fragmented records, unclear eligibility, department boundaries and slow status updates without claiming measured performance improvements.

## 4. Project objectives
The objectives are secure identity, structured placement data, department-aware access and transparent application progress.

## 5. Existing workflow and proposed platform
This qualitative comparison shows how CampusHire replaces scattered workflows with connected records, backend scope checks and traceable round outcomes.

## 6. Roles and access boundaries
STUDENT owns profile and application actions. DTPO operates within the assigned department. UTPO has university-wide scope and manages coordinators. The backend derives scope from the authenticated account.

## 7. Technology stack
Flutter serves students, Angular serves staff, Spring Boot exposes the REST API, PostgreSQL stores the domain data, and JWT plus BCrypt provide the security foundation.

## 8. System architecture
Both clients call the Spring Boot API. The API applies authentication, authorization, eligibility and persistence rules before accessing PostgreSQL.

## 9. Use case diagram
The three actors and their main use cases are shown inside the CampusHire system boundary. Students browse, apply and track progress; staff manage companies, drives and rounds; UTPOs also manage DTPO coordinators.

## 10. Activity diagram
The student path authenticates, loads published eligible drives, validates eligibility and duplicate protection, then persists an application. The staff path continues with rounds, results and final outcomes.

## 11. Class diagram
The class view highlights the current domain relationships: users and optional student profiles, departments, companies, drives, programs, applications, recruitment rounds and round results.

## 12. Functional modules
The modules are identity, student profile, companies, placement drives, applications, recruitment rounds and dashboards.

## 13. Database design
The model shows the central placement tables and their relationships. It intentionally emphasizes the workflow rather than every database column.

## 14. Authentication and user management
Student login uses enrollment number; staff login uses university email through the identifier contract. BCrypt verifies passwords, JWT carries identity context, and protected services reload current account state. Staff do not require student rows.

## 15. Placement drive management
The drive lifecycle is company, draft, publish, eligible visibility and close. Students never see drafts, and backend rules remain authoritative.

## 16. Student application workflow
The Flutter client calls the existing student drive, apply and application-history APIs. The backend enforces published status, eligibility and the unique student-drive application constraint.

## 17. Recruitment round management
The recruitment model supports ordered rounds, candidate results, published results and final outcomes. The V5 migration and runtime rollout still require controlled validation before being described as live.

## 18. UTPO and DTPO dashboards
The staff dashboard uses real placement APIs where available. DTPO scope is enforced server-side; UTPO has university-wide access and coordinator management.

## 19. Testing and verification
Java 21 compilation and test compilation passed. Focused Flutter tests passed and Flutter analysis reported only style information. Angular type-check passed. The full backend suite and Angular production build remain environment-blocked by the documented Mockito/Byte Buddy and Node-process issues.

## 20. Current limitations and next steps
The next safe steps are controlled V5 rollout, environment-supported backend tests, Angular production-build verification and approved data readiness for academic years, departments and programs.

## 21. Conclusion
CampusHire provides a secure, traceable foundation for university placement operations. The diagrams make the system actors, workflow and domain relationships explicit for project review and future implementation planning.
