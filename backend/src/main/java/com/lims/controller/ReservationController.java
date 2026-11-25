package com.lims.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

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

    public record ReservationRequest(@NotNull Long memberId, @NotNull Long bookId) {}

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
    public Map<String, Object> createReservation(@Valid @RequestBody ReservationRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO reservations(member_id, book_id) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, request.memberId());
            statement.setLong(2, request.bookId());
            return statement;
        }, keyHolder);
        return Map.of("reservationId", keyHolder.getKey().longValue(), "message", "Reservation created");
    }

    @PostMapping("/{id}/cancel")
    public Map<String, String> cancelReservation(@PathVariable Long id) {
        int changed = jdbc.update("""
            UPDATE reservations SET reservation_status = 'CANCELLED'
            WHERE reservation_id = ? AND reservation_status = 'PENDING'
            """, id);
        if (changed == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Pending reservation not found");
        }
        return Map.of("message", "Reservation cancelled");
    }
}
