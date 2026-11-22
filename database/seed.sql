USE lims_db;

INSERT INTO roles(role_name) VALUES
('ADMIN'), ('LIBRARIAN'), ('MEMBER');

INSERT INTO member_types(type_name, max_books, borrowing_days, fine_per_day) VALUES
('STUDENT', 3, 14, 5.00),
('TEACHER', 5, 30, 5.00),
('GENERAL', 2, 14, 5.00);

INSERT INTO categories(category_name, description) VALUES
('Computer Science', 'Computing, software and information technology'),
('Mathematics', 'Pure and applied mathematics'),
('Literature', 'Fiction, poetry and literary works');

INSERT INTO publishers(publisher_name, address) VALUES
('Sample Academic Press', 'Sample address');

INSERT INTO authors(author_name, country) VALUES
('Sample Author', 'Bangladesh');

INSERT INTO users(role_id, full_name, email, password_hash)
VALUES (1, 'System Administrator', 'admin@example.com', 'REPLACE_WITH_BCRYPT_HASH');

INSERT INTO members(user_id, member_type_id, join_date)
VALUES (1, 1, CURRENT_DATE);

INSERT INTO books(publisher_id, category_id, isbn, title, edition, publication_year)
VALUES (1, 1, '9780000000001', 'Introduction to Computer Science', '1st', 2024);

INSERT INTO book_authors(book_id, author_id) VALUES (1, 1);

INSERT INTO book_copies(book_id, accession_number, copy_status, condition_status) VALUES
(1, 'ACC-0001', 'AVAILABLE', 'GOOD'),
(1, 'ACC-0002', 'AVAILABLE', 'GOOD');
