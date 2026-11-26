package com.lims.controller;

import com.lims.service.FineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    private final FineService fineService;

    public FineController(JdbcTemplate jdbc, FineService fineService) {
        this.jdbc = jdbc;
        this.fineService = fineService;
    }

    public record PaymentRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String method
    ) {}

    public record WaiveRequest(
        @DecimalMin("0.01") BigDecimal amount,
        String reason
    ) {}

    @GetMapping
    public List<Map<String, Object>> getFineBalances() {
        return jdbc.queryForList("SELECT * FROM vw_fine_balances ORDER BY fine_id DESC");
    }

    @PostMapping("/{id}/payments")
    public Map<String, String> recordPayment(@PathVariable @Positive Long id, @Valid @RequestBody PaymentRequest request) {
        if (request.amount() == null || request.method() == null || request.method().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Amount and payment method are required");
        }
        jdbc.update("CALL sp_record_fine_payment(?, ?, ?)", id, request.amount(), request.method());
        return Map.of("message", "Payment recorded");
    }

    @PostMapping("/{id}/waive")
    public Map<String, Object> waiveFine(@PathVariable @Positive Long id, @RequestBody(required = false) WaiveRequest request) {
        BigDecimal amount = request == null ? null : request.amount();
        return fineService.waiveFine(id, amount);
    }

    @GetMapping("/{id}/payments")
    public List<Map<String, Object>> getPayments(@PathVariable @Positive Long id) {
        return fineService.payments(id);
    }

    @GetMapping("/member/{memberId}")
    public Map<String, Object> getMemberFines(@PathVariable @Positive Long memberId) {
        return fineService.finesForMember(memberId);
    }
}
