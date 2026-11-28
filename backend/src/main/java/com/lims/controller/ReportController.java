package com.lims.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class ReportController {
    private static final Set<String> EXPORTABLE = Set.of(
        "inventory", "overdue", "member-activity", "fine-collection",
        "most-borrowed", "reservations", "borrowing-history");

    private final JdbcTemplate jdbc;

    public ReportController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/inventory")
    public List<Map<String, Object>> inventory() {
        return jdbc.queryForList("SELECT * FROM vw_available_books ORDER BY title");
    }

    @GetMapping("/borrowing-history")
    public List<Map<String, Object>> borrowingHistory() {
        return jdbc.queryForList("""
            SELECT br.borrowing_id, u.full_name AS member_name, b.title,
                   bc.accession_number, br.issue_date, br.due_date, br.return_date,
                   br.renewal_count, br.borrowing_status
            FROM borrowings br
            JOIN members m ON m.member_id = br.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN book_copies bc ON bc.copy_id = br.copy_id
            JOIN books b ON b.book_id = bc.book_id
            ORDER BY br.borrowing_id DESC
            """);
    }

    @GetMapping("/overdue")
    public List<Map<String, Object>> overdue() {
        return jdbc.queryForList("SELECT * FROM vw_overdue_books ORDER BY days_overdue DESC");
    }

    @GetMapping("/member-activity")
    public List<Map<String, Object>> memberActivity() {
        return jdbc.queryForList("SELECT * FROM vw_member_activity ORDER BY member_name");
    }

    @GetMapping("/fine-collection")
    public List<Map<String, Object>> fineCollection() {
        return jdbc.queryForList("""
            SELECT p.payment_id, p.payment_date, p.payment_method, p.amount_paid,
                   f.fine_id, u.full_name AS member_name, b.title
            FROM payments p
            JOIN fines f ON f.fine_id = p.fine_id
            JOIN borrowings br ON br.borrowing_id = f.borrowing_id
            JOIN members m ON m.member_id = br.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN book_copies bc ON bc.copy_id = br.copy_id
            JOIN books b ON b.book_id = bc.book_id
            ORDER BY p.payment_date DESC, p.payment_id DESC
            """);
    }

    @GetMapping("/fine-collection/summary")
    public List<Map<String, Object>> fineCollectionSummary() {
        return jdbc.queryForList("""
            SELECT payment_method, COUNT(*) AS payments, SUM(amount_paid) AS total_collected
            FROM payments
            GROUP BY payment_method
            ORDER BY payment_method
            """);
    }

    @GetMapping("/most-borrowed")
    public List<Map<String, Object>> mostBorrowed() {
        return jdbc.queryForList(
            "SELECT * FROM vw_most_borrowed_books ORDER BY borrow_count DESC LIMIT 20");
    }

    @GetMapping("/reservations")
    public List<Map<String, Object>> reservations() {
        return jdbc.queryForList("""
            SELECT r.reservation_id, u.full_name AS member_name, b.title,
                   r.reservation_date, r.reservation_status
            FROM reservations r
            JOIN members m ON m.member_id = r.member_id
            JOIN users u ON u.user_id = m.user_id
            JOIN books b ON b.book_id = r.book_id
            ORDER BY r.reservation_date DESC
            """);
    }

    /**
     * CSV export for the tabular reports. The file opens directly in Excel;
     * no PDF generation is performed.
     */
    @GetMapping(value = "/{name}/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@PathVariable String name) {
        if (!EXPORTABLE.contains(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        }
        List<Map<String, Object>> rows = switch (name) {
            case "inventory" -> inventory();
            case "overdue" -> overdue();
            case "member-activity" -> memberActivity();
            case "fine-collection" -> fineCollection();
            case "most-borrowed" -> mostBorrowed();
            case "reservations" -> reservations();
            case "borrowing-history" -> borrowingHistory();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        };
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + name + "-report.csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(toCsv(name, rows));
    }

    /**
     * Column order of each exportable report. Used when a report has no rows:
     * the CSV then still carries its header row instead of being a 0-byte file.
     * Keep in step with the SELECT lists / views above.
     */
    private static final Map<String, List<String>> REPORT_COLUMNS = Map.of(
        "inventory", List.of("book_id", "isbn", "title", "category_name", "total_copies", "available_copies"),
        "overdue", List.of("borrowing_id", "member_id", "member_name", "member_email", "member_phone",
            "book_id", "title", "accession_number", "issue_date", "due_date", "days_overdue"),
        "borrowing-history", List.of("borrowing_id", "member_name", "title", "accession_number",
            "issue_date", "due_date", "return_date", "renewal_count", "borrowing_status"),
        "member-activity", List.of("member_id", "member_name", "member_type", "total_borrowings",
            "active_borrowings", "overdue_borrowings", "total_fines", "unpaid_balance"),
        "fine-collection", List.of("payment_id", "payment_date", "payment_method", "amount_paid",
            "fine_id", "member_name", "title"),
        "most-borrowed", List.of("book_id", "title", "isbn", "borrow_count"),
        "reservations", List.of("reservation_id", "member_name", "title", "reservation_date", "reservation_status")
    );

    private static String toCsv(String name, List<Map<String, Object>> rows) {
        List<String> columns = (rows == null || rows.isEmpty())
            ? REPORT_COLUMNS.getOrDefault(name, List.of())
            : List.copyOf(rows.get(0).keySet());
        if (columns.isEmpty()) {
            return "";
        }
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", columns.stream().map(ReportController::escape).toList())).append("\r\n");
        if (rows == null) {
            return csv.toString();
        }
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    csv.append(',');
                }
                Object value = row.get(columns.get(i));
                csv.append(escape(value == null ? "" : value.toString()));
            }
            csv.append("\r\n");
        }
        return csv.toString();
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
