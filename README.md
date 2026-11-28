# Library Information Management System (LIMS)

A university DBMS project built with MySQL 8, Java 17, Spring Boot, and a responsive HTML/CSS/JavaScript frontend.

## Modules

- Book catalogue: search, create, update and delete APIs, with author and
  category/publisher/availability filters plus paged advanced search
- Authors, categories and publishers: lookup and management APIs
- Book copies: inventory by accession number, copy status and condition
- Members: registration, CSV bulk import from admission data, borrowing
  history and library clearance
- Borrowing: issue, renewal and return through transactional stored procedures
- Reservations: create, list, cancel, queue positions, fulfilment, expiry
  and a ready-for-pickup list
- Fines and payments: balance view, payment procedure, full/partial waivers
- Dashboard and reports: library statistics, report endpoints and CSV export
- Authentication: email/password login with BCrypt hashes and role-based
  authorization (ADMIN, LIBRARIAN, MEMBER, STUDENT, TEACHER, GENERAL)
- Audit log of all write operations
- AI library assistant: chat box that finds books, shows a member's own loans and fines,
  recommends books and explains library rules (`POST /api/ai/chat`; needs the
  `AI_API_KEY` environment variable)

## Technology

- MySQL 8
- Java 17 and Spring Boot 3 (Maven)
- HTML, CSS and JavaScript
- Postman and MySQL Workbench
- GitHub Actions for backend verification

## Quick start

1. Run the SQL scripts in `database/` in this order: `schema.sql`, `seed.sql`, `views.sql`, `procedures.sql`, `triggers.sql`, then `database/migrations/002_completion_features.sql` and `database/migrations/003_ai_assistant.sql`. (Migration `001` is only for databases created from the older schema; a fresh `schema.sql` already includes it.)
2. Configure `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in your environment.
3. Run `mvn clean verify` and `mvn spring-boot:run` from `backend/`.
4. Serve `frontend/` with a local static server on port 5500 and log in with a demo account (see the status doc).

See [Project Status and API guide](docs/PROJECT_STATUS.md) for the full endpoint list, demo accounts and limitations.

## Security notice

The backend requires login for every endpoint except public registration,
and restricts write operations to ADMIN and LIBRARIAN accounts. Passwords
are stored as BCrypt hashes. Demo accounts with well-known passwords are
created by migration 002 for coursework purposes — change or remove them
before any real use, and never use real personal data in sample records.
