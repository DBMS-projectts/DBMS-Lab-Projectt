USE lims_db;

DELIMITER $$

CREATE PROCEDURE sp_issue_book(
  IN p_member_id BIGINT,
  IN p_copy_id BIGINT,
  IN p_issued_by BIGINT
)
BEGIN
  DECLARE v_member_status VARCHAR(20);
  DECLARE v_max_books INT;
  DECLARE v_loan_days INT;
  DECLARE v_loan_months INT;
  DECLARE v_active_count INT;
  DECLARE v_copy_status VARCHAR(20);
  DECLARE v_borrowing_id BIGINT;
  DECLARE v_due_date DATE;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT m.membership_status, mt.max_books, mt.borrowing_days, mt.borrowing_months
    INTO v_member_status, v_max_books, v_loan_days, v_loan_months
  FROM members m
  JOIN member_types mt ON mt.member_type_id = m.member_type_id
  WHERE m.member_id = p_member_id
  FOR UPDATE;

  IF v_member_status <> 'ACTIVE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Member is not active';
  END IF;

  SELECT copy_status INTO v_copy_status
  FROM book_copies
  WHERE copy_id = p_copy_id
  FOR UPDATE;

  IF v_copy_status <> 'AVAILABLE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Book copy is not available';
  END IF;

  SELECT COUNT(*) INTO v_active_count
  FROM borrowings
  WHERE member_id = p_member_id
    AND borrowing_status IN ('ACTIVE','OVERDUE');

  IF v_active_count >= v_max_books THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Member has reached the borrowing limit';
  END IF;

  IF v_loan_months > 0 THEN
    SET v_due_date = DATE_ADD(CURRENT_DATE, INTERVAL v_loan_months MONTH);
  ELSE
    SET v_due_date = DATE_ADD(CURRENT_DATE, INTERVAL v_loan_days DAY);
  END IF;

  INSERT INTO borrowings(member_id, copy_id, issued_by, issue_date, due_date, borrowing_status)
  VALUES (p_member_id, p_copy_id, p_issued_by, CURRENT_DATE, v_due_date, 'ACTIVE');

  SET v_borrowing_id = LAST_INSERT_ID();

  UPDATE book_copies SET copy_status = 'BORROWED' WHERE copy_id = p_copy_id;

  COMMIT;
  SELECT v_borrowing_id AS borrowing_id, v_due_date AS due_date;
END$$

CREATE PROCEDURE sp_renew_book(IN p_borrowing_id BIGINT)
BEGIN
  DECLARE v_status VARCHAR(20);
  DECLARE v_due_date DATE;
  DECLARE v_renewal_count INT;
  DECLARE v_loan_days INT;
  DECLARE v_loan_months INT;
  DECLARE v_new_due_date DATE;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT b.borrowing_status, b.due_date, b.renewal_count,
         mt.borrowing_days, mt.borrowing_months
    INTO v_status, v_due_date, v_renewal_count, v_loan_days, v_loan_months
  FROM borrowings b
  JOIN members m ON m.member_id = b.member_id
  JOIN member_types mt ON mt.member_type_id = m.member_type_id
  WHERE b.borrowing_id = p_borrowing_id
  FOR UPDATE;

  IF v_status <> 'ACTIVE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only active borrowings can be renewed';
  END IF;

  IF v_due_date < CURRENT_DATE THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Overdue books cannot be renewed';
  END IF;

  IF v_renewal_count >= 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Maximum two renewals allowed';
  END IF;

  IF v_loan_months > 0 THEN
    SET v_new_due_date = DATE_ADD(v_due_date, INTERVAL v_loan_months MONTH);
  ELSE
    SET v_new_due_date = DATE_ADD(v_due_date, INTERVAL v_loan_days DAY);
  END IF;

  UPDATE borrowings
  SET due_date = v_new_due_date,
      renewal_count = renewal_count + 1
  WHERE borrowing_id = p_borrowing_id;

  COMMIT;
  SELECT p_borrowing_id AS borrowing_id,
         v_new_due_date AS due_date,
         v_renewal_count + 1 AS renewal_count;
END$$

CREATE PROCEDURE sp_return_book(IN p_borrowing_id BIGINT)
BEGIN
  DECLARE v_status VARCHAR(20);
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT borrowing_status INTO v_status
  FROM borrowings
  WHERE borrowing_id = p_borrowing_id
  FOR UPDATE;

  IF v_status NOT IN ('ACTIVE','OVERDUE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Borrowing is not currently active';
  END IF;

  UPDATE borrowings
  SET return_date = CURRENT_DATE,
      borrowing_status = 'RETURNED'
  WHERE borrowing_id = p_borrowing_id;

  UPDATE book_copies bc
  JOIN borrowings b ON b.copy_id = bc.copy_id
  SET bc.copy_status = 'AVAILABLE'
  WHERE b.borrowing_id = p_borrowing_id;

  COMMIT;
END$$

CREATE PROCEDURE sp_record_fine_payment(
  IN p_fine_id BIGINT,
  IN p_amount DECIMAL(10,2),
  IN p_method VARCHAR(30)
)
BEGIN
  DECLARE v_fine_amount DECIMAL(10,2);
  DECLARE v_paid DECIMAL(10,2);
  DECLARE v_remaining DECIMAL(10,2);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT fine_amount INTO v_fine_amount
  FROM fines WHERE fine_id = p_fine_id
  FOR UPDATE;

  SELECT COALESCE(SUM(amount_paid),0) INTO v_paid
  FROM payments WHERE fine_id = p_fine_id;

  SET v_remaining = v_fine_amount - v_paid;

  IF p_amount <= 0 OR p_amount > v_remaining THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Payment amount is invalid';
  END IF;

  INSERT INTO payments(fine_id, amount_paid, payment_method)
  VALUES (p_fine_id, p_amount, p_method);

  SET v_paid = v_paid + p_amount;

  UPDATE fines
  SET fine_status = CASE
    WHEN v_paid >= v_fine_amount THEN 'PAID'
    ELSE 'PARTIALLY_PAID'
  END
  WHERE fine_id = p_fine_id;

  COMMIT;
END$$

DELIMITER ;
