USE lims_db;

-- Make this script safe to re-run during development.
DROP TRIGGER IF EXISTS trg_borrowings_before_insert;
DROP TRIGGER IF EXISTS trg_borrowings_after_insert;
DROP TRIGGER IF EXISTS trg_borrowings_after_update;

DELIMITER $$

CREATE TRIGGER trg_borrowings_before_insert
BEFORE INSERT ON borrowings
FOR EACH ROW
BEGIN
  DECLARE v_copy_status VARCHAR(20);
  DECLARE v_condition_status VARCHAR(20);
  DECLARE v_member_status VARCHAR(20);
  DECLARE v_max_books INT;
  DECLARE v_active_count INT;

  SELECT copy_status, condition_status
    INTO v_copy_status, v_condition_status
  FROM book_copies
  WHERE copy_id = NEW.copy_id;

  IF v_copy_status <> 'AVAILABLE' OR v_condition_status = 'DAMAGED' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Book copy is not available for borrowing';
  END IF;

  SELECT m.membership_status, mt.max_books
    INTO v_member_status, v_max_books
  FROM members m
  JOIN member_types mt ON mt.member_type_id = m.member_type_id
  WHERE m.member_id = NEW.member_id;

  IF v_member_status <> 'ACTIVE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Member is not active';
  END IF;

  SELECT COUNT(*) INTO v_active_count
  FROM borrowings
  WHERE member_id = NEW.member_id
    AND borrowing_status IN ('ACTIVE','OVERDUE');

  IF v_active_count >= v_max_books THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Borrowing limit exceeded';
  END IF;
END$$

CREATE TRIGGER trg_borrowings_after_insert
AFTER INSERT ON borrowings
FOR EACH ROW
BEGIN
  UPDATE book_copies
  SET copy_status = 'BORROWED'
  WHERE copy_id = NEW.copy_id;
END$$

CREATE TRIGGER trg_borrowings_after_update
AFTER UPDATE ON borrowings
FOR EACH ROW
BEGIN
  DECLARE v_fine_per_day DECIMAL(8,2);
  DECLARE v_days_overdue INT;

  IF OLD.return_date IS NULL
     AND NEW.return_date IS NOT NULL
     AND NEW.borrowing_status = 'RETURNED' THEN

    UPDATE book_copies
    SET copy_status = CASE
      WHEN condition_status = 'DAMAGED' THEN 'DAMAGED'
      ELSE 'AVAILABLE'
    END
    WHERE copy_id = NEW.copy_id;

    SELECT mt.fine_per_day INTO v_fine_per_day
    FROM members m
    JOIN member_types mt ON mt.member_type_id = m.member_type_id
    WHERE m.member_id = NEW.member_id;

    SET v_days_overdue = GREATEST(DATEDIFF(NEW.return_date, NEW.due_date), 0);

    IF v_days_overdue > 0 THEN
      INSERT INTO fines(borrowing_id, fine_amount, fine_status)
      VALUES (NEW.borrowing_id, v_days_overdue * v_fine_per_day, 'UNPAID');
    END IF;
  END IF;
END$$

DELIMITER ;
