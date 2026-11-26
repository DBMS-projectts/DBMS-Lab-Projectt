# Project Status

The Library Information Management System is feature-complete for the
CSE-2424 DBMS Lab scope: catalogue, members, circulation, reservations,
fines, dashboard, reports and role-based login are all implemented.

## Implemented in repository

- MySQL schema for roles, users, member types, members, publishers,
  categories, authors, books, book authors, copies, borrowings,
  reservations, fines and payments.
- Seed data plus SQL views, stored procedures and triggers.
- Migration `001` (borrowing rules + renewals) and migration `002`
  (cover images, `NOT_FOR_LOAN` copies, member roles, audit log,
  reporting views, demo accounts).
- Spring Boot 3 / Java 17 Maven backend:
  - JPA book CRUD with title/ISBN search, multi-field filters
    (author, ISBN, category, publisher, availability) and a paged
    advanced-search endpoint.
  - Author CRUD, including the books of an author.
  - Book copy inventory: register copies, find by accession number,
    update copy status and condition.
  - Member management: create/list/update/deactivate members, borrowing
    history, CSV bulk import from admission data, and library clearance.
  - Borrowing issue/renew/return endpoints backed by stored procedures.
  - Reservation create/list/cancel plus queue positions, fulfilment,
    automatic expiry and a ready-for-pickup list.
  - Fine balances, payments (stored procedure), full/partial waivers,
    payment history and per-member fine totals.
  - Dashboard statistics and report endpoints with CSV export.
  - Audit log of every write request (admin endpoint to read it).
- Authentication and role-based authorization (Spring Security, HTTP
  Basic, BCrypt password hashes, stateless sessions).
- Centralized API error responses (ProblemDetail).
- Frontend: login/logout, dashboard stat cards, catalogue search,
  add-book form (with authors and cover image) and delete action.
- GitHub Actions workflow to run Maven verification for backend changes.

## API endpoints

### Authentication

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | /api/auth/register | Public | Register a user; member roles also create a membership |
| GET | /api/auth/me | Logged in | Current user profile (incl. membership) |
| PUT | /api/auth/profile | Logged in | Update own name and phone |
| POST | /api/auth/change-password | Logged in | Change own password |

### Books, authors, categories, publishers, copies

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | /api/books | Logged in | List books (supports `?search=`) |
| GET | /api/books?author=&isbn=&categoryId=&publisherId=&availableOnly= | Logged in | Filtered catalogue |
| GET | /api/books/search?page=&size=&sort= | Logged in | Advanced search with pagination |
| GET | /api/books/{id} | Logged in | Book details (authors + cover image) |
| POST | /api/books | ADMIN, LIBRARIAN | Add a book |
| PUT | /api/books/{id} | ADMIN, LIBRARIAN | Update a book |
| DELETE | /api/books/{id} | ADMIN, LIBRARIAN | Delete if no dependent records |
| GET | /api/authors | Logged in | List authors (`?search=` supported) |
| GET | /api/authors/{id} | Logged in | Author details |
| GET | /api/authors/{id}/books | Logged in | Books written by the author |
| POST / PUT / DELETE | /api/authors[/{id}] | ADMIN, LIBRARIAN | Manage authors |
| GET | /api/categories | Logged in | List categories |
| GET | /api/publishers | Logged in | List publishers |
| GET | /api/copies?bookId= | Logged in | List copies, optionally per book |
| GET | /api/copies/{id} | Logged in | Copy details |
| GET | /api/copies/by-accession/{accessionNumber} | Logged in | Find a copy by barcode/accession |
| POST | /api/copies | ADMIN, LIBRARIAN | Register a copy |
| PUT | /api/copies/{id}/status | ADMIN, LIBRARIAN | Set AVAILABLE / DAMAGED / LOST / NOT_FOR_LOAN |
| PUT | /api/copies/{id}/condition | ADMIN, LIBRARIAN | Set GOOD / FAIR / DAMAGED |

### Members

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | /api/members | ADMIN, LIBRARIAN | Create a user + membership |
| GET | /api/members | ADMIN, LIBRARIAN | List members with active-loan counts |
| GET | /api/members/{id} | ADMIN, LIBRARIAN | Member details |
| PUT | /api/members/{id} | ADMIN, LIBRARIAN | Change type, expiry date, status |
| DELETE | /api/members/{id} | ADMIN, LIBRARIAN | Deactivate (suspend + disable login) |
| GET | /api/members/{id}/history | ADMIN, LIBRARIAN | Full borrowing history with fines |
| GET | /api/members/my-history | Logged in | Own borrowing history |
| POST | /api/members/import | ADMIN, LIBRARIAN | CSV bulk import (admission data) |
| POST | /api/members/{id}/clearance | ADMIN, LIBRARIAN | Grant library clearance |

CSV import expects a header row `full_name,email,phone,member_type` with an
optional `join_date` (YYYY-MM-DD) column. Imported accounts receive the
default password `lims12345`, which members must change after first login
(via `POST /api/auth/change-password`). Duplicate emails and unknown member
types are skipped and reported in the response.

Clearance follows the real library rule confirmed in the field interview:
it is granted only when the member has no book on loan and no unpaid fine
balance; the membership then becomes EXPIRED and the login is deactivated.

### Circulation, reservations, fines

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | /api/borrowings | Logged in | Active borrowing list |
| POST | /api/borrowings/issue | ADMIN, LIBRARIAN | Issue a copy (stored procedure) |
| POST | /api/borrowings/{id}/renew | ADMIN, LIBRARIAN | Renew a loan (max 2 renewals) |
| POST | /api/borrowings/{id}/return | ADMIN, LIBRARIAN | Return a book |
| GET | /api/reservations | Logged in | List reservations |
| POST | /api/reservations | Logged in | Create a reservation |
| POST | /api/reservations/{id}/cancel | Logged in | Cancel a pending reservation |
| GET | /api/reservations/queue/{bookId} | Logged in | Waiting queue with positions |
| POST | /api/reservations/{id}/fulfill | Logged in | Mark a reservation fulfilled |
| POST | /api/reservations/expire | Logged in | Expire pending requests older than 7 days |
| GET | /api/reservations/ready | Logged in | Pending reservations whose book is available |
| GET | /api/fines | Logged in | Fine balances |
| POST | /api/fines/{id}/payments | ADMIN, LIBRARIAN | Record a fine payment (stored procedure) |
| POST | /api/fines/{id}/waive | ADMIN, LIBRARIAN | Waive a fine fully or partly |
| GET | /api/fines/{id}/payments | Logged in | Payment history of a fine |
| GET | /api/fines/member/{memberId} | Logged in | A member's fines and total outstanding |

Fine waivers are recorded as payment rows with method `WAIVER`, so balances
remain auditable. A fine settled with waiver help ends in status `WAIVED`;
a partially waived fine stays `PARTIALLY_PAID`.

### Dashboard, reports, admin

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | /api/dashboard/stats | ADMIN, LIBRARIAN | Library totals and outstanding amounts |
| GET | /api/reports/inventory | ADMIN, LIBRARIAN | Copies per book with availability |
| GET | /api/reports/borrowing-history | ADMIN, LIBRARIAN | All loans, newest first |
| GET | /api/reports/overdue | ADMIN, LIBRARIAN | Overdue loans with member contact |
| GET | /api/reports/member-activity | ADMIN, LIBRARIAN | Per-member totals and unpaid balance |
| GET | /api/reports/fine-collection | ADMIN, LIBRARIAN | Every payment, newest first |
| GET | /api/reports/fine-collection/summary | ADMIN, LIBRARIAN | Collections grouped by payment method |
| GET | /api/reports/most-borrowed | ADMIN, LIBRARIAN | Top 20 most borrowed books |
| GET | /api/reports/reservations | ADMIN, LIBRARIAN | Reservation list |
| GET | /api/reports/{name}/export | ADMIN, LIBRARIAN | CSV download for a report |
| GET | /api/admin/audit-logs | ADMIN | Last 200 write operations |

`{name}` for CSV export is one of: `inventory`, `overdue`,
`member-activity`, `fine-collection`, `most-borrowed`, `reservations`,
`borrowing-history`.

## Authentication model

- Login is the account email + password, sent as HTTP Basic credentials.
- Passwords are stored only as BCrypt hashes (`users.password_hash`).
- The account role becomes a Spring Security authority `ROLE_<role>`:
  `ADMIN`, `LIBRARIAN`, `MEMBER`, `STUDENT`, `TEACHER`, `GENERAL`.
- A deactivated account (`users.is_active = 0`) cannot log in.
- Read endpoints require any logged-in user; write operations on the
  catalogue, members, circulation, fines, dashboard and reports require
  `ADMIN` or `LIBRARIAN`; the audit log requires `ADMIN`.
- Sessions are stateless; the demo frontend stores the Basic token in
  `sessionStorage` only.

## Demo accounts (sample data — coursework only)

Created by `database/migrations/002_completion_features.sql`:

| Email | Password | Role | Notes |
|---|---|---|---|
| admin@lims.local | admin123 | ADMIN | Full access incl. audit log |
| librarian@lims.local | libra123 | LIBRARIAN | Staff operations |
| student@lims.local | student123 | STUDENT | Has a STUDENT membership |

Change or delete these accounts before any real use. The older seed user
`admin@example.com` still has the placeholder hash from `seed.sql` and
cannot log in until a real BCrypt hash is set.

## Local setup

1. Install MySQL 8 and Java 17.
2. In MySQL Workbench, execute these files **in this order**:
   - `database/schema.sql`
   - `database/seed.sql`
   - `database/views.sql`
   - `database/procedures.sql`
   - `database/triggers.sql`
   - `database/migrations/001_borrowing_rules_and_renewals.sql`
     (existing databases created from the older schema only — a fresh
     `schema.sql` already contains these changes)
   - `database/migrations/002_completion_features.sql`
     (required: the backend validates the `books.cover_image_url` column,
     the audit table and the new views at startup)
3. Set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` for your local MySQL
   account. Do not commit credentials.
4. From the `backend/` folder run `mvn clean verify`, then
   `mvn spring-boot:run`. The API listens on port 8080.
5. Serve the `frontend/` folder on port 5500 (for example with VS Code
   Live Server) and open `index.html`. Log in with a demo account to load
   the catalogue and dashboard.

## Important limitations / verification

- GitHub Actions is configured to compile and verify the backend on every
  push to `main`; inspect its run status before treating the build as
  passed.
- SQL scripts still need to be executed in a clean MySQL 8 database and
  tested end-to-end with the backend.
- Bulk import accepts **CSV only** — Excel `.xlsx` files are not parsed
  (save the admission sheet as CSV first). Quoted CSV fields containing
  commas are not supported.
- Report export is **CSV only**. The file opens in Excel, but no formatted
  spreadsheet or PDF report is generated.
- Reservation "notify when available" is the ready-for-pickup list
  (`GET /api/reservations/ready`) that staff work from — the system does
  not send email or SMS notifications.
- Overdue state is computed in the views (`vw_active_borrowings`,
  `vw_overdue_books`) from `due_date`; the stored
  `borrowings.borrowing_status` stays `ACTIVE` until the book is returned
  (the return trigger then assesses the fine).
- The frontend covers login, dashboard, catalogue and book entry. Member,
  circulation, reservation and fine operations are API-only (Postman).
- Do not use real personal data in sample records.
