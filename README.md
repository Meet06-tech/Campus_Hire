# Placement Management System — V1

This repository contains the first runnable skeleton of the Placement Management System.

## Current scope

- Spring Boot backend in `backend/`
- Flutter application shell in `flutter_app/`
- PostgreSQL configuration through environment variables
- Backend health endpoint: `GET /api/health`
- Student registration and login endpoints
- BCrypt password hashing and JWT authentication foundation
- A `user_accounts` PostgreSQL table created automatically by Hibernate
- No placement business features yet

## Java 21 setup

This project targets Java 21. Before running Maven commands in a terminal, use:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
java -version
mvn -version
```

Both Java commands should report version 21.

## Run the backend

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

Then open <http://localhost:8080/api/health>.

Optional database variables for later database-backed features:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/placement_management
export DB_USERNAME=postgres
export DB_PASSWORD=your-local-password
export JWT_SECRET=replace-with-a-long-random-local-secret
```

Secrets should stay in your local shell or an uncommitted `.env` file.

Provisioned staff email login is allowlisted separately. Set the exact approved
domain or comma-separated approved domains; do not use a leading wildcard or
substring. If unset or blank, staff email login is disabled:

```bash
export STAFF_ALLOWED_EMAIL_DOMAINS=charusat.ac.in
```

Authentication endpoints:

- `POST /api/auth/register` with `fullName`, `enrollmentNumber`, `universityEmail`, and `password`
- `POST /api/auth/login` with `enrollmentNumber` and `password` for students,
  or `identifier` and `password` for provisioned staff accounts

Both successful requests return a JWT token. Send it on later protected requests as `Authorization: Bearer <token>`.

## Student profile photos

Profile photos are stored by the backend outside the source tree under
`./var/uploads/profile-photos` by default. Set `PROFILE_PHOTO_STORAGE_DIR` to
an absolute or deployment-specific directory when needed. The server accepts
validated JPEG and PNG images up to 5 MB; the database stores only a generated
relative photo key. The upload directory is intentionally git-ignored.

Before starting the backend after this feature is applied, the database owner
must run `backend/src/main/resources/db/migration/V3__add_student_profile_photo_key.sql`
against the existing database. Hibernate remains configured with
`ddl-auto=validate` and will not create the column automatically.

## Run the Flutter shell

```bash
cd flutter_app
flutter pub get
flutter run
```

## Next feature

After this skeleton is verified, the first business feature will be authentication: the database user model, registration/login API, and the matching Flutter screens.

