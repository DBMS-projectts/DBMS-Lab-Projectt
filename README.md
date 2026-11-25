# Library Information Management System (LIMS)

A university DBMS project built with MySQL 8, Java 17, Spring Boot, and a responsive HTML/CSS/JavaScript frontend.

## Modules

- Book catalogue: search, create, update and delete APIs
- Categories and publishers: lookup APIs
- Borrowing: issue and return through transactional stored procedures
- Reservations: create, list and cancel
- Fines and payments: balance view and payment procedure

## Technology

- MySQL 8
- Java 17 and Spring Boot 3 (Maven)
- HTML, CSS and JavaScript
- Postman and MySQL Workbench
- GitHub Actions for backend verification

## Quick start

1. Run the SQL scripts in `database/` in this order: `schema.sql`, `seed.sql`, `views.sql`, `procedures.sql`, `triggers.sql`.
2. Configure `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in your environment.
3. Run `mvn clean verify` and `mvn spring-boot:run` from `backend/`.
4. Serve `frontend/` with a local static server on port 5500.

See [Project Status and API guide](docs/PROJECT_STATUS.md) for endpoints and limitations.

## Security notice

The current backend security configuration permits requests for local academic development. Authentication and role-based authorization are not implemented. Do not deploy this configuration publicly or use real personal data.
