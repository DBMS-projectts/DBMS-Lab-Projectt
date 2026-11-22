USE lims_db;

CREATE OR REPLACE VIEW vw_available_books AS
SELECT b.book_id, b.isbn, b.title, c.category_name,
       COUNT(bc.copy_id) AS total_copies,
       SUM(CASE WHEN bc.copy_status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_copies
FROM books b
JOIN categories c ON c.category_id = b.category_id
LEFT JOIN book_copies bc ON bc.book_id = b.book_id
GROUP BY b.book_id, b.isbn, b.title, c.category_name;

CREATE OR REPLACE VIEW vw_active_borrowings AS
SELECT br.borrowing_id, m.member_id, u.full_name AS member_name,
       b.title, bc.accession_number, br.issue_date, br.due_date,
       CASE WHEN br.due_date < CURRENT_DATE THEN 'OVERDUE' ELSE 'ACTIVE' END AS current_status
FROM borrowings br
JOIN members m ON m.member_id = br.member_id
JOIN users u ON u.user_id = m.user_id
JOIN book_copies bc ON bc.copy_id = br.copy_id
JOIN books b ON b.book_id = bc.book_id
WHERE br.return_date IS NULL;

CREATE OR REPLACE VIEW vw_fine_balances AS
SELECT f.fine_id, f.borrowing_id, f.fine_amount,
       COALESCE(SUM(p.amount_paid),0) AS total_paid,
       f.fine_amount - COALESCE(SUM(p.amount_paid),0) AS balance_due,
       f.fine_status
FROM fines f
LEFT JOIN payments p ON p.fine_id = f.fine_id
GROUP BY f.fine_id, f.borrowing_id, f.fine_amount, f.fine_status;
