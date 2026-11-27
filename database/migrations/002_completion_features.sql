USE lims_db;

-- ============================================================
-- Migration 002: completion features
-- Run AFTER schema.sql, seed.sql, views.sql, procedures.sql,
-- triggers.sql (and after 001 on older databases).
-- ============================================================

-- 1. Book cover images (used by the catalogue UI).
ALTER TABLE books
  ADD COLUMN cover_image_url VARCHAR(500) NULL AFTER publication_year;

-- 2. Allow NOT_FOR_LOAN copies (reference / restricted copies that are
--    visible in the catalogue but never issued, as in the real library).
ALTER TABLE book_copies DROP CHECK chk_copy_status;
ALTER TABLE book_copies
  ADD CONSTRAINT chk_copy_status
  CHECK (copy_status IN ('AVAILABLE','BORROWED','DAMAGED','LOST','NOT_FOR_LOAN'));

-- 3. Roles aligned with member types so self-registration can assign a
--    meaningful role (seed.sql only created ADMIN, LIBRARIAN, MEMBER).
INSERT INTO roles(role_name)
SELECT 'STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'STUDENT');

INSERT INTO roles(role_name)
SELECT 'TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'TEACHER');

INSERT INTO roles(role_name)
SELECT 'GENERAL'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'GENERAL');

-- 4. Audit trail for write operations (populated by AuditInterceptor).
CREATE TABLE IF NOT EXISTS audit_logs (
  audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(150),
  action VARCHAR(10) NOT NULL,
  request_path VARCHAR(255),
  status_code INT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Per-member activity summary (report: member activity).
CREATE OR REPLACE VIEW vw_member_activity AS
SELECT m.member_id,
       u.full_name AS member_name,
       mt.type_name AS member_type,
       (SELECT COUNT(*) FROM borrowings b
         WHERE b.member_id = m.member_id) AS total_borrowings,
       (SELECT COUNT(*) FROM borrowings b
         WHERE b.member_id = m.member_id
           AND b.return_date IS NULL) AS active_borrowings,
       (SELECT COUNT(*) FROM borrowings b
         WHERE b.member_id = m.member_id
           AND b.return_date IS NULL
           AND b.due_date < CURRENT_DATE) AS overdue_borrowings,
       COALESCE((SELECT SUM(f.fine_amount)
                   FROM fines f
                   JOIN borrowings b ON b.borrowing_id = f.borrowing_id
                  WHERE b.member_id = m.member_id), 0) AS total_fines,
       COALESCE((SELECT SUM(fb.balance_due)
                   FROM vw_fine_balances fb
                   JOIN borrowings b ON b.borrowing_id = fb.borrowing_id
                  WHERE b.member_id = m.member_id
                    AND fb.fine_status IN ('UNPAID','PARTIALLY_PAID')), 0) AS unpaid_balance
FROM members m
JOIN users u ON u.user_id = m.user_id
JOIN member_types mt ON mt.member_type_id = m.member_type_id;

-- 6. Most borrowed books (report).
CREATE OR REPLACE VIEW vw_most_borrowed_books AS
SELECT b.book_id, b.title, b.isbn, COUNT(br.borrowing_id) AS borrow_count
FROM books b
JOIN book_copies bc ON bc.book_id = b.book_id
JOIN borrowings br ON br.copy_id = bc.copy_id
GROUP BY b.book_id, b.title, b.isbn;

-- 7. Overdue loans with member contact details (report + follow-up).
CREATE OR REPLACE VIEW vw_overdue_books AS
SELECT br.borrowing_id,
       m.member_id,
       u.full_name AS member_name,
       u.email AS member_email,
       u.phone AS member_phone,
       b.book_id,
       b.title,
       bc.accession_number,
       br.issue_date,
       br.due_date,
       DATEDIFF(CURRENT_DATE, br.due_date) AS days_overdue
FROM borrowings br
JOIN members m ON m.member_id = br.member_id
JOIN users u ON u.user_id = m.user_id
JOIN book_copies bc ON bc.copy_id = br.copy_id
JOIN books b ON b.book_id = bc.book_id
WHERE br.return_date IS NULL
  AND br.due_date < CURRENT_DATE;

-- 8. Demo accounts (SAMPLE DATA for coursework only - change or remove
--    before any real use). Passwords are BCrypt hashes:
--      admin@lims.local     / admin123    (ADMIN)
--      librarian@lims.local / libra123    (LIBRARIAN)
--      student@ugrad.iiuc.ac.bd   / student123  (STUDENT role + STUDENT member)
INSERT INTO users(role_id, full_name, email, password_hash, phone)
SELECT r.role_id, 'Demo Administrator', 'admin@lims.local',
       '$2b$12$KNwcO4CC2qDp0X7NePpPEeW5/qB9vgv.DTXyePPQQ2X2jN4yFrRQ2', '01000000001'
FROM roles r
WHERE r.role_name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'admin@lims.local');

INSERT INTO users(role_id, full_name, email, password_hash, phone)
SELECT r.role_id, 'Demo Librarian', 'librarian@lims.local',
       '$2b$12$pGrixFD6Q9DYlxX3Pw.P0uTyNX2.2H7tkBMETIDgKPFSNqEM.1zfK', '01000000002'
FROM roles r
WHERE r.role_name = 'LIBRARIAN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'librarian@lims.local');

INSERT INTO users(role_id, full_name, email, password_hash, phone)
SELECT r.role_id, 'Demo Student', 'student@ugrad.iiuc.ac.bd',
       '$2b$12$MH.dT2CP7AtOd4wVjfvrwO8/v85dga8XraIHlMDQarNCNN4QOONYe', '01000000003'
FROM roles r
WHERE r.role_name = 'STUDENT'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'student@ugrad.iiuc.ac.bd');

INSERT INTO members(user_id, member_type_id, join_date)
SELECT u.user_id, mt.member_type_id, CURRENT_DATE
FROM users u, member_types mt
WHERE u.email = 'student@ugrad.iiuc.ac.bd'
  AND mt.type_name = 'STUDENT'
  AND NOT EXISTS (SELECT 1 FROM members m WHERE m.user_id = u.user_id);
