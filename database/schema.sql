CREATE DATABASE IF NOT EXISTS lims_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE lims_db;

CREATE TABLE roles (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(25),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

CREATE TABLE member_types (
  member_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_name VARCHAR(50) NOT NULL UNIQUE,
  max_books INT NOT NULL,
  borrowing_days INT NOT NULL,
  fine_per_day DECIMAL(8,2) NOT NULL DEFAULT 0,
  CONSTRAINT chk_member_max_books CHECK (max_books > 0),
  CONSTRAINT chk_member_borrowing_days CHECK (borrowing_days > 0),
  CONSTRAINT chk_member_fine CHECK (fine_per_day >= 0)
);

CREATE TABLE members (
  member_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  member_type_id BIGINT NOT NULL,
  join_date DATE NOT NULL,
  expiry_date DATE,
  membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_members_type FOREIGN KEY (member_type_id) REFERENCES member_types(member_type_id),
  CONSTRAINT chk_membership_status CHECK (membership_status IN ('ACTIVE','EXPIRED','SUSPENDED')),
  CONSTRAINT chk_membership_dates CHECK (expiry_date IS NULL OR expiry_date >= join_date)
);

CREATE TABLE publishers (
  publisher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  publisher_name VARCHAR(150) NOT NULL UNIQUE,
  address VARCHAR(255)
);

CREATE TABLE categories (
  category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500)
);

CREATE TABLE authors (
  author_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  author_name VARCHAR(150) NOT NULL,
  country VARCHAR(80)
);

CREATE TABLE books (
  book_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  publisher_id BIGINT,
  category_id BIGINT NOT NULL,
  isbn VARCHAR(20) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  edition VARCHAR(50),
  publication_year SMALLINT,
  CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES publishers(publisher_id),
  CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
  CONSTRAINT chk_publication_year CHECK (publication_year IS NULL OR publication_year BETWEEN 1400 AND 2100)
);

CREATE TABLE book_authors (
  book_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  PRIMARY KEY (book_id, author_id),
  CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
  CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE CASCADE
);

CREATE TABLE book_copies (
  copy_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  accession_number VARCHAR(50) NOT NULL UNIQUE,
  copy_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  condition_status VARCHAR(20) NOT NULL DEFAULT 'GOOD',
  CONSTRAINT fk_copies_book FOREIGN KEY (book_id) REFERENCES books(book_id),
  CONSTRAINT chk_copy_status CHECK (copy_status IN ('AVAILABLE','BORROWED','DAMAGED','LOST')),
  CONSTRAINT chk_copy_condition CHECK (condition_status IN ('GOOD','FAIR','DAMAGED'))
);

CREATE TABLE borrowings (
  borrowing_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id BIGINT NOT NULL,
  copy_id BIGINT NOT NULL,
  issued_by BIGINT NOT NULL,
  issue_date DATE NOT NULL,
  due_date DATE NOT NULL,
  return_date DATE,
  borrowing_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT fk_borrowings_member FOREIGN KEY (member_id) REFERENCES members(member_id),
  CONSTRAINT fk_borrowings_copy FOREIGN KEY (copy_id) REFERENCES book_copies(copy_id),
  CONSTRAINT fk_borrowings_issuer FOREIGN KEY (issued_by) REFERENCES users(user_id),
  CONSTRAINT chk_borrowing_dates CHECK (due_date >= issue_date),
  CONSTRAINT chk_return_date CHECK (return_date IS NULL OR return_date >= issue_date),
  CONSTRAINT chk_borrowing_status CHECK (borrowing_status IN ('ACTIVE','RETURNED','OVERDUE'))
);

CREATE TABLE reservations (
  reservation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reservation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  CONSTRAINT fk_reservations_member FOREIGN KEY (member_id) REFERENCES members(member_id),
  CONSTRAINT fk_reservations_book FOREIGN KEY (book_id) REFERENCES books(book_id),
  CONSTRAINT chk_reservation_status CHECK (reservation_status IN ('PENDING','FULFILLED','CANCELLED','EXPIRED'))
);

CREATE TABLE fines (
  fine_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  borrowing_id BIGINT NOT NULL UNIQUE,
  fine_amount DECIMAL(10,2) NOT NULL,
  fine_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  assessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fines_borrowing FOREIGN KEY (borrowing_id) REFERENCES borrowings(borrowing_id),
  CONSTRAINT chk_fine_amount CHECK (fine_amount >= 0),
  CONSTRAINT chk_fine_status CHECK (fine_status IN ('UNPAID','PARTIALLY_PAID','PAID','WAIVED'))
);

CREATE TABLE payments (
  payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fine_id BIGINT NOT NULL,
  amount_paid DECIMAL(10,2) NOT NULL,
  payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payment_method VARCHAR(30) NOT NULL,
  CONSTRAINT fk_payments_fine FOREIGN KEY (fine_id) REFERENCES fines(fine_id),
  CONSTRAINT chk_payment_amount CHECK (amount_paid > 0)
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_borrowings_member ON borrowings(member_id);
CREATE INDEX idx_borrowings_copy_status ON borrowings(copy_id, borrowing_status);
CREATE INDEX idx_reservations_book_status ON reservations(book_id, reservation_status);
