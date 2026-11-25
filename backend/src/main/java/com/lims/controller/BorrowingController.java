package com.lims.controller;

import jakarta.validation.constraints.NotNull;
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

    public record IssueRequest(@NotNull Long memberId, @NotNull Long copyId, @NotNull Long issuedBy) {}

    @GetMapping
    public List<Map<String, Object>> getActiveBorrowings() {
        return jdbc.queryForList("SELECT * FROM vw_active_borrowings ORDER BY due_date");
    }

    @PostMapping("/issue")
    public Map<String, Object> issueBook(@RequestBody IssueRequest request) {
        return jdbc.queryForMap("CALL sp_issue_book(?, ?, ?)",
            request.memberId(), request.copyId(), request.issuedBy());
    }

    @PostMapping("/{id}/return")
    public Map<String, String> returnBook(@PathVariable Long id) {
        jdbc.execute("CALL sp_return_book(" + id + ")");
        return Map.of("message", "Book returned successfully");
    }
}
