package com.lims.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
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
        @NotNull @Positive Long copyId
    ) {}

    @GetMapping
    public List<Map<String, Object>> getActiveBorrowings() {
        return jdbc.queryForList("SELECT * FROM vw_active_borrowings ORDER BY due_date");
    }

    @PostMapping("/issue")
    public Map<String, Object> issueBook(@Valid @RequestBody IssueRequest request,
                                         Authentication authentication) {
        Long authenticatedStaffId = jdbc.queryForObject(
            "SELECT user_id FROM users WHERE email = ?", Long.class, authentication.getName());
        if (authenticatedStaffId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authenticated staff account not found");
        }
        return jdbc.queryForMap("CALL sp_issue_book(?, ?, ?)",
            request.memberId(), request.copyId(), authenticatedStaffId);
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
