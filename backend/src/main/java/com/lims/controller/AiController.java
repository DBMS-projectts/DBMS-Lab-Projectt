package com.lims.controller;

import com.lims.service.AiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Any logged-in user may chat; SecurityConfig's anyRequest().authenticated() already covers /api/ai/**. */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    public record ChatRequest(@NotBlank @Size(max = 500) String question) {}

    @PostMapping("/chat")
    public Map<String, Object> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        boolean staff = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        return aiService.chat(authentication.getName(), staff, request.question().trim());
    }
}
