package com.lims.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fine operations that go beyond sp_record_fine_payment: full or partial
 * waivers (the librarian confirmed fines are sometimes partly waived),
 * payment history and per-member balances. Waivers are recorded as payment
 * rows with method WAIVER so every balance stays auditable.
 */
@Service
@Transactional
public class FineService {
    private final JdbcTemplate jdbc;

    public FineService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> waiveFine(Long fineId, BigDecimal amount) {
        Map<String, Object> fine = fineBalance(fineId);
        String status = (String) fine.get("fine_status");
        if ("PAID".equals(status) || "WAIVED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This fine is already settled");
        }
        BigDecimal fineAmount = toBigDecimal(fine.get("fine_amount"));
        BigDecimal totalPaid = toBigDecimal(fine.get("total_paid"));
        BigDecimal balance = toBigDecimal(fine.get("balance_due"));
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This fine has no outstanding balance");
        }
        BigDecimal waiveAmount = amount == null ? balance : amount;
        if (waiveAmount.compareTo(BigDecimal.ZERO) <= 0 || waiveAmount.compareTo(balance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Waiver amount must be greater than zero and no more than the balance " + balance.toPlainString());
        }
        jdbc.update("INSERT INTO payments(fine_id, amount_paid, payment_method) VALUES (?, ?, 'WAIVER')",
            fineId, waiveAmount);

        BigDecimal covered = totalPaid.add(waiveAmount);
        String newStatus;
        if (covered.compareTo(fineAmount) >= 0) {
            // Settled with help from a waiver entry: WAIVED unless real
            // payments alone already covered the whole fine.
            newStatus = totalPaid.compareTo(fineAmount) >= 0 ? "PAID" : "WAIVED";
        } else {
            newStatus = "PARTIALLY_PAID";
        }
        jdbc.update("UPDATE fines SET fine_status = ? WHERE fine_id = ?", newStatus, fineId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fineId", fineId);
        result.put("waivedAmount", waiveAmount);
        result.put("fineStatus", newStatus);
        result.put("message", "Fine waiver recorded");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> payments(Long fineId) {
        fineBalance(fineId);
        return jdbc.queryForList("""
            SELECT payment_id, amount_paid, payment_date, payment_method
            FROM payments
            WHERE fine_id = ?
            ORDER BY payment_id
            """, fineId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> finesForMember(Long memberId) {
        List<Map<String, Object>> fines = jdbc.queryForList("""
            SELECT fb.fine_id, fb.borrowing_id, fb.fine_amount, fb.total_paid,
                   fb.balance_due, fb.fine_status, b.title
            FROM vw_fine_balances fb
            JOIN borrowings br ON br.borrowing_id = fb.borrowing_id
            JOIN book_copies bc ON bc.copy_id = br.copy_id
            JOIN books b ON b.book_id = bc.book_id
            WHERE br.member_id = ?
            ORDER BY fb.fine_id DESC
            """, memberId);
        BigDecimal outstanding = fines.stream()
            .filter(row -> "UNPAID".equals(row.get("fine_status")) || "PARTIALLY_PAID".equals(row.get("fine_status")))
            .map(row -> toBigDecimal(row.get("balance_due")))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memberId", memberId);
        result.put("totalOutstanding", outstanding);
        result.put("fines", fines);
        return result;
    }

    private Map<String, Object> fineBalance(Long fineId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM vw_fine_balances WHERE fine_id = ?", fineId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fine not found");
        }
        return rows.get(0);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(value.toString());
    }
}
