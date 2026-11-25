# Project Status

## Implemented in repository

- MySQL schema for roles, users, member types, members, publishers, categories, authors, books, copies, borrowings, reservations, fines and payments.
- Seed data plus SQL views, stored procedures and triggers.
- Spring Boot 3 / Java 17 Maven backend with JPA book CRUD and search.
- Read-only category and publisher APIs.
- Borrowing issue/return endpoints backed by stored procedures.
- Reservation create/list/cancel endpoints.
- Fine balance and payment endpoints backed by SQL views/procedures.
- Centralized API error responses and local-development security configuration.
- Frontend catalogue search, add-book form and delete action.
- GitHub Actions workflow to run Maven verification for backend changes.

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/books | List books |
| GET | /api/books?search=java | Search title or ISBN |
| GET | /api/books/{id} | Book details |
| POST | /api/books | Add a book |
| PUT | /api/books/{id} | Update a book |
| DELETE | /api/books/{id} | Delete if no dependent records |
| GET | /api/categories | List categories |
| GET | /api/publishers | List publishers |
| GET | /api/borrowings | Active borrowing list |
| POST | /api/borrowings/issue | Issue a copy |
| POST | /api/borrowings/{id}/return | Return a book |
| GET | /api/reservations | List reservations |
| POST | /api/reservations | Create a reservation |
| POST | /api/reservations/{id}/cancel | Cancel pending reservation |
| GET | /api/fines | Fine balances |
| POST | /api/fines/{id}/payments | Record a fine payment |

## Local setup

1. Install MySQL 8 and Java 17.
2. In MySQL Workbench, execute these files in order:
   - database/schema.sql
   - database/seed.sql
   - database/views.sql
   - database/procedures.sql
   - database/triggers.sql
3. Set DB_URL, DB_USERNAME and DB_PASSWORD for your local MySQL account. Do not commit credentials.
4. From the backend folder run `mvn clean verify`, then `mvn spring-boot:run`.
5. Serve the frontend folder on port 5500 (for example with VS Code Live Server) and open its index.html.

## Important limitations / verification

- GitHub Actions is configured to compile and verify the backend; inspect its run status before treating the build as passed.
- SQL scripts still need to be executed in a clean MySQL 8 database and tested end-to-end with the backend.
- Authentication and real role-based authorization are not implemented. The current security policy is for local academic development only; do not expose this application publicly.
- The UI currently provides catalogue search, add and delete. Member, borrowing, reservation and fine screens are not yet implemented.
- Do not use real personal data in sample records.
