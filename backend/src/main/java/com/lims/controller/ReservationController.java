package com.lims.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class ReservationController {
    private final JdbcTemplate jdbc;

    public ReservationController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ReservationRequest(Long memberId, @NotNull Long bookId) {}

    @GetMapping
    public List<Map<String, Object>> getReservations() {
        return jdbc.queryForList("""
            SELECT r.reservation_id, r.member_id, u.full_name AS member_name,
                   r.book_id, b.title, r.reservation_date, r.reservation_status
            FROM reservations r
            JOIN members m ON m.member_id = r.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN books b ON b.book_id = r.book_id
            ORDER BY r.reservation_date DESC
            """);
    }

    @PostMapping
    public Map<String, Object> createReservation(@Valid @RequestBody ReservationRequest request,
                                                  Authentication authentication) {
        boolean staff = isStaff(authentication);
        Long memberId;
        if (staff) {
            if (request.memberId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberId is required for staff-created reservations");
            }
            memberId = request.memberId();
        } else {
            memberId = currentMemberId(authentication);
            if (request.memberId() != null && !request.memberId().equals(memberId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only reserve for your own account");
            }
        }

        Integer pending = jdbc.queryForObject("""
            SELECT COUNT(*) FROM reservations
            WHERE member_id = ? AND book_id = ? AND reservation_status = 'PENDING'
            """, Integer.class, memberId, request.bookId());
        if (pending != null && pending > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A pending reservation already exists for this member and book");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO reservations(member_id, book_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, memberId);
                statement.setLong(2, request.bookId());
                return statement;
            }, keyHolder);
        } catch (DataIntegrityViolationException ex) {
            // Backstop for the unique key in migration 005, in case two requests
            // race past the check above.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A pending reservation already exists for this member and book");
        }
        return Map.of("reservationId", keyHolder.getKey().longValue(), "message", "Reservation created");
    }

    /** The signed-in member's own reservations, newest first. */
    @GetMapping("/my")
    public List<Map<String, Object>> getMyReservations(Authentication authentication) {
        Long memberId = currentMemberId(authentication);
        return jdbc.queryForList("""
            SELECT r.reservation_id, r.member_id, u.full_name AS member_name,
                   r.book_id, b.title, r.reservation_date, r.reservation_status
            FROM reservations r
            JOIN members m ON m.member_id = r.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN books b ON b.book_id = r.book_id
            WHERE r.member_id = ?
            ORDER BY r.reservation_date DESC
            """, memberId);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, String> cancelReservation(@PathVariable Long id, Authentication authentication) {
        Map<String, Object> reservation = findReservation(id);
        if (!isStaff(authentication)) {
            Long memberId = currentMemberId(authentication);
            if (!memberId.equals(((Number) reservation.get("member_id")).longValue())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only cancel your own reservations");
            }
        }
        requirePending(reservation);
        jdbc.update("UPDATE reservations SET reservation_status = 'CANCELLED' WHERE reservation_id = ?", id);
        return Map.of("message", "Reservation cancelled");
    }

    /** Waiting queue for one book, oldest request first, with queue position. */
    @GetMapping("/queue/{bookId}")
    public List<Map<String, Object>> getQueue(@PathVariable Long bookId) {
        return jdbc.queryForList("""
            SELECT r.reservation_id, r.member_id, u.full_name AS member_name,
                   r.book_id, b.title, r.reservation_date, r.reservation_status,
                   (SELECT COUNT(*) FROM reservations r2
                     WHERE r2.book_id = r.book_id
                       AND r2.reservation_status = 'PENDING'
                       AND (r2.reservation_date < r.reservation_date
                            OR (r2.reservation_date = r.reservation_date
                                AND r2.reservation_id <= r.reservation_id))) AS queue_position
            FROM reservations r
            JOIN members m ON m.member_id = r.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN books b ON b.book_id = r.book_id
            WHERE r.book_id = ? AND r.reservation_status = 'PENDING'
            ORDER BY r.reservation_date ASC, r.reservation_id ASC
            """, bookId);
    }

    @PostMapping("/{id}/fulfill")
    public Map<String, String> fulfillReservation(@PathVariable Long id) {
        requirePending(findReservation(id));
        jdbc.update("UPDATE reservations SET reservation_status = 'FULFILLED' WHERE reservation_id = ?", id);
        return Map.of("message", "Reservation fulfilled");
    }

    private Map<String, Object> findReservation(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT member_id, reservation_status FROM reservations WHERE reservation_id = ?", id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        return rows.get(0);
    }

    /** A reservation that exists but is no longer PENDING is a state conflict
        (409), not "not found" (404) — the old code reported both as 404. */
    private void requirePending(Map<String, Object> reservation) {
        String status = String.valueOf(reservation.get("reservation_status"));
        if (!"PENDING".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Reservation is " + status + " and can no longer be changed");
        }
    }

    /** Pending requests older than seven days expire automatically. */
    @PostMapping("/expire")
    public Map<String, Object> expireOldReservations() {
        int changed = jdbc.update("""
            UPDATE reservations SET reservation_status = 'EXPIRED'
            WHERE reservation_status = 'PENDING'
              AND reservation_date < NOW() - INTERVAL 7 DAY
            """);
        return Map.of("expired", changed, "message", "Expired stale pending reservations");
    }

    @GetMapping("/ready")
    public List<Map<String, Object>> getReadyReservations() {
        return jdbc.queryForList("""
            SELECT r.reservation_id, r.member_id, u.full_name AS member_name,
                   r.book_id, b.title, r.reservation_date, ab.available_copies
            FROM reservations r
            JOIN members m ON m.member_id = r.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN books b ON b.book_id = r.book_id
            JOIN vw_available_books ab ON ab.book_id = r.book_id
            WHERE r.reservation_status = 'PENDING' AND ab.available_copies > 0
            ORDER BY r.reservation_date ASC
            """);
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                || authority.getAuthority().equals("ROLE_LIBRARIAN"));
    }

    private Long currentMemberId(Authentication authentication) {
        List<Long> ids = jdbc.query(
            "SELECT member_id FROM members m JOIN users u ON u.user_id = m.user_id WHERE u.email = ?",
            (rs, rowNum) -> rs.getLong("member_id"), authentication.getName());
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has no library membership");
        }
        return ids.get(0);
    }
}
