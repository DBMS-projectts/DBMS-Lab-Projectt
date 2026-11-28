USE lims_db;

INSERT INTO roles(role_name) VALUES
('ADMIN'), ('LIBRARIAN'), ('MEMBER');

INSERT INTO member_types(type_name, max_books, borrowing_days, borrowing_months, fine_per_day) VALUES
('STUDENT', 4, 15, 0, 1.00),
('ADMIN_STAFF', 4, 30, 0, 1.00),
('TEACHER', 1, 0, 6, 1.00),
('GENERAL', 2, 14, 0, 1.00);

INSERT INTO categories(category_name, description) VALUES
('Computer Science', 'Computing, software and information technology'),
('Software Engineering', 'Software design, development, testing and maintenance'),
('Database Management', 'Database systems, SQL, data modelling and administration'),
('Artificial Intelligence', 'Artificial intelligence concepts and applications'),
('Machine Learning', 'Machine learning methods and practical applications'),
('Web Development', 'Frontend, backend and full-stack web development'),
('Networking', 'Computer networks and communication systems'),
('Cyber Security', 'Information security, privacy and ethical hacking'),
('Mathematics', 'Pure and applied mathematics'),
('Physics', 'Classical and modern physics'),
('Chemistry', 'General, organic and applied chemistry'),
('Literature', 'Fiction, poetry and literary works'),
('History', 'World, regional and cultural history'),
('Islamic Studies', 'Islamic history, culture and scholarship'),
('Business Studies', 'Business, management, finance and economics');

INSERT INTO publishers(publisher_name, address) VALUES
('Sample Academic Press', 'Sample address'),
('Pearson', NULL),
('McGraw Hill', NULL),
('Oxford University Press', NULL),
('Cambridge University Press', NULL),
('Wiley', NULL),
('Springer Nature', NULL),
('Packt Publishing', NULL),
('O''Reilly Media', NULL),
('Prentice Hall', NULL),
('CRC Press', NULL),
('Cengage', NULL),
('MIT Press', NULL);

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
