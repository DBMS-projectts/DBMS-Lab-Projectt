USE lims_db;

-- Apply this migration only to an existing database created from the older schema.
ALTER TABLE member_types
  ADD COLUMN borrowing_months INT NOT NULL DEFAULT 0 AFTER borrowing_days;

UPDATE member_types
SET max_books = 4, borrowing_days = 15, borrowing_months = 0, fine_per_day = 1.00
WHERE type_name = 'STUDENT';

UPDATE member_types
SET max_books = 4, borrowing_days = 30, borrowing_months = 0, fine_per_day = 1.00
WHERE type_name IN ('ADMIN_STAFF', 'ADMINISTRATIVE_STAFF');

UPDATE member_types
SET max_books = 1, borrowing_days = 0, borrowing_months = 6, fine_per_day = 1.00
WHERE type_name = 'TEACHER';

INSERT INTO member_types(type_name, max_books, borrowing_days, borrowing_months, fine_per_day)
SELECT 'ADMIN_STAFF', 4, 30, 0, 1.00
WHERE NOT EXISTS (
  SELECT 1 FROM member_types WHERE type_name = 'ADMIN_STAFF'
);

UPDATE member_types
SET fine_per_day = 1.00
WHERE type_name = 'GENERAL';

ALTER TABLE borrowings
  ADD COLUMN renewal_count INT NOT NULL DEFAULT 0 AFTER return_date;
