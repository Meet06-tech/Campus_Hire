# Placement Management System — V1

This repository contains the first runnable skeleton of the Placement Management System.

## Current scope

- Spring Boot backend in `backend/`
- Flutter application shell in `flutter_app/`
- PostgreSQL configuration through environment variables
- Backend health endpoint: `GET /api/health`
- No login, business rules, or database tables yet

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
```

Secrets should stay in your local shell or an uncommitted `.env` file.

## Run the Flutter shell

```bash
cd flutter_app
flutter pub get
flutter run
```

## Next feature

After this skeleton is verified, the first business feature will be authentication: the database user model, registration/login API, and the matching Flutter screens.

