package com.lims.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class AdminController {
    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/audit-logs")
    public List<Map<String, Object>> auditLogs() {
        return jdbc.queryForList("SELECT * FROM audit_logs ORDER BY audit_id DESC LIMIT 200");
    }
}
