package com.lims.controller;

import com.lims.dto.ImportResult;
import com.lims.dto.MemberCreateRequest;
import com.lims.dto.MemberResponse;
import com.lims.dto.MemberUpdateRequest;
import com.lims.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse createMember(@Valid @RequestBody MemberCreateRequest request) {
        return memberService.create(request);
    }

    @GetMapping
    public List<MemberResponse> getMembers() {
        return memberService.list();
    }

    @GetMapping("/my-history")
    public List<Map<String, Object>> myHistory(Authentication authentication) {
        return memberService.myHistory(authentication.getName());
    }

    @GetMapping("/{id}")
    public MemberResponse getMember(@PathVariable Long id) {
        return memberService.get(id);
    }

    @PutMapping("/{id}")
    public MemberResponse updateMember(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateMember(@PathVariable Long id) {
        memberService.deactivate(id);
    }

    @GetMapping("/{id}/history")
    public List<Map<String, Object>> memberHistory(@PathVariable Long id) {
        return memberService.history(id);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importMembers(@RequestParam("file") MultipartFile file) {
        return memberService.importCsv(file);
    }

    @PostMapping("/{id}/clearance")
    public Map<String, Object> clearance(@PathVariable Long id) {
        return memberService.clearance(id);
    }
}
