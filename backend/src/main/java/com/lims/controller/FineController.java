package com.lims.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fines")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class FineController {
    private final JdbcTemplate jdbc;

    public FineController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record PaymentRequest(BigDecimal amount, String method) {}

    @GetMapping
    public List<Map<String, Object>> getFineBalances() {
        return jdbc.queryForList("SELECT * FROM vw_fine_balances ORDER BY fine_id DESC");
    }

    @PostMapping("/{id}/payments")
    public Map<String, String> recordPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
        if (request.amount() == null || request.method() == null || request.method().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Amount and payment method are required");
        }
        jdbc.update("CALL sp_record_fine_payment(?, ?, ?)", id, request.amount(), request.method());
        return Map.of("message", "Payment recorded");
    }
}
