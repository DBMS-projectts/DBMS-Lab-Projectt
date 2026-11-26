package com.lims.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrowings")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class BorrowingController {
    private final JdbcTemplate jdbc;

    public BorrowingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record IssueRequest(
        @NotNull @Positive Long memberId,
        @NotNull @Positive Long copyId,
        @NotNull @Positive Long issuedBy
    ) {}

    @GetMapping
    public List<Map<String, Object>> getActiveBorrowings() {
        return jdbc.queryForList("SELECT * FROM vw_active_borrowings ORDER BY due_date");
    }

    @PostMapping("/issue")
    public Map<String, Object> issueBook(@Valid @RequestBody IssueRequest request) {
        return jdbc.queryForMap("CALL sp_issue_book(?, ?, ?)",
            request.memberId(), request.copyId(), request.issuedBy());
    }

    @PostMapping("/{id}/renew")
    public Map<String, Object> renewBook(@PathVariable @Positive Long id) {
        return jdbc.queryForMap("CALL sp_renew_book(?)", id);
    }

    @PostMapping("/{id}/return")
    public Map<String, String> returnBook(@PathVariable @Positive Long id) {
        jdbc.update("CALL sp_return_book(?)", id);
        return Map.of("message", "Book returned successfully");
    }
}
