# Project Status

## Implemented in repository

- Initial MySQL schema, sample data, views, triggers and stored procedures.
- Spring Boot Maven project skeleton targeting Java 17.
- Initial JPA entities and read-only book listing/search API.
- Responsive frontend catalogue that calls the book API.

## Not implemented or verified yet

- Complete CRUD for all entities.
- Authentication, password hashing, and role-based authorization.
- Member, borrowing, return, reservation, fine, and payment APIs.
- Automated integration tests and SQL execution against a real MySQL server.
- Deployment configuration and production security review.

## Local setup (initial)

1. Create the MySQL database by running `database/schema.sql`.
2. Run the other SQL files in order: `seed.sql`, `views.sql`, `procedures.sql`, then `triggers.sql`.
3. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as environment variables if your local credentials differ.
4. From the `backend` folder, run `mvn spring-boot:run`.
5. Open `frontend/index.html` using a local static web server. The frontend expects the API at `http://localhost:8080/api`.

> This is an in-progress academic project. The SQL and application have not yet been verified together. Do not use real personal data or deploy this configuration publicly.
