USE lims_db;

-- One member may hold only ONE pending reservation per book.
--
-- MySQL unique indexes ignore NULL values, so the generated key below only
-- "exists" while a reservation is PENDING; cancelled, fulfilled and expired
-- rows all map to NULL and never collide with each other.
--
-- If this fails with a duplicate-key error while building the index, the
-- table still contains live duplicate PENDING rows: cancel the extras first
-- (keep the oldest reservation_id per member/book), then re-run.

ALTER TABLE reservations
  ADD COLUMN pending_key VARCHAR(45)
    GENERATED ALWAYS AS (
      CASE WHEN reservation_status = 'PENDING'
           THEN CONCAT(member_id, ':', book_id)
           ELSE NULL
      END
    ) STORED,
  ADD UNIQUE KEY uq_reservations_pending (pending_key);
