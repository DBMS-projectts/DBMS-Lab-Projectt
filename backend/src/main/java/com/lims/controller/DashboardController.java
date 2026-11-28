package com.lims.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class DashboardController {
    private final JdbcTemplate jdbc;

    public DashboardController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBooks", count("SELECT COUNT(*) FROM books"));
        stats.put("totalCopies", count("SELECT COUNT(*) FROM book_copies"));
        stats.put("availableCopies", count(
            "SELECT COUNT(*) FROM book_copies WHERE copy_status = 'AVAILABLE'"));
        stats.put("borrowedCopies", count(
            "SELECT COUNT(*) FROM book_copies WHERE copy_status = 'BORROWED'"));
        stats.put("totalMembers", count("SELECT COUNT(*) FROM members"));
        stats.put("activeMembers", count(
            "SELECT COUNT(*) FROM members WHERE membership_status = 'ACTIVE'"));
        stats.put("activeBorrowings", count(
            "SELECT COUNT(*) FROM borrowings WHERE return_date IS NULL"));
        stats.put("overdueBorrowings", count(
            "SELECT COUNT(*) FROM borrowings WHERE return_date IS NULL AND due_date < CURRENT_DATE"));
        stats.put("pendingReservations", count(
            "SELECT COUNT(*) FROM reservations WHERE reservation_status = 'PENDING'"));
        stats.put("totalFineCollected", jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount_paid), 0) FROM payments WHERE payment_method <> 'WAIVER'", Object.class));
        stats.put("totalFineWaived", jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount_paid), 0) FROM payments WHERE payment_method = 'WAIVER'", Object.class));
        stats.put("totalFineOutstanding", jdbc.queryForObject("""
            SELECT COALESCE(SUM(balance_due), 0) FROM vw_fine_balances
            WHERE fine_status IN ('UNPAID','PARTIALLY_PAID')
            """, Object.class));
        return stats;
    }

    private Long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
