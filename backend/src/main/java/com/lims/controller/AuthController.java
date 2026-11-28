package com.lims.controller;

import com.lims.dto.ChangePasswordRequest;
import com.lims.dto.ProfileResponse;
import com.lims.dto.RegisterRequest;
import com.lims.dto.UpdateProfileRequest;
import com.lims.entity.Member;
import com.lims.entity.MemberType;
import com.lims.entity.Role;
import com.lims.entity.User;
import com.lims.repository.MemberRepository;
import com.lims.repository.MemberTypeRepository;
import com.lims.repository.RoleRepository;
import com.lims.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class AuthController {
    /** Public registration must never grant staff or administrator privileges. */
    private static final Set<String> PUBLIC_REGISTRATION_ROLES =
        Set.of("STUDENT", "TEACHER", "GENERAL", "MEMBER");
    private static final Set<String> MEMBER_ROLES = PUBLIC_REGISTRATION_ROLES;

    private final UserRepository users;
    private final RoleRepository roles;
    private final MemberTypeRepository memberTypes;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository users, RoleRepository roles,
                          MemberTypeRepository memberTypes, MemberRepository members,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.memberTypes = memberTypes;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProfileResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        String roleName = request.roleName().trim().toUpperCase();
        if (!PUBLIC_REGISTRATION_ROLES.contains(roleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "This role cannot be assigned through public registration");
        }
        Role role = roles.findByName(roleName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + roleName));

        MemberType memberType = null;
        if (MEMBER_ROLES.contains(roleName)) {
            if (request.memberTypeName() == null || request.memberTypeName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "memberTypeName is required for role " + roleName);
            }
            String typeName = request.memberTypeName().trim().toUpperCase();
            memberType = memberTypes.findByTypeName(typeName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown member type: " + typeName));
        }

        User user = new User();
        user.setRole(role);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone() == null ? null : request.phone().trim());
        user.setActive(true);
        user = users.save(user);

        Member member = null;
        if (memberType != null) {
            member = new Member();
            member.setUser(user);
            member.setMemberType(memberType);
            member.setJoinDate(LocalDate.now());
            member.setStatus("ACTIVE");
            member = members.save(member);
        }
        return toProfile(user, member);
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ProfileResponse me(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Member member = members.findByUser(user).orElse(null);
        return toProfile(user, member);
    }

    @PutMapping("/profile")
    @Transactional
    public ProfileResponse updateProfile(Authentication authentication,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        User user = getCurrentUser(authentication);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone() == null ? null : request.phone().trim());
        user = users.save(user);
        Member member = members.findByUser(user).orElse(null);
        return toProfile(user, member);
    }

    @PostMapping("/change-password")
    @Transactional
    public Map<String, String> changePassword(Authentication authentication,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        User user = getCurrentUser(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        return Map.of("message", "Password changed");
    }

    private User getCurrentUser(Authentication authentication) {
        return users.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
    }

    private ProfileResponse toProfile(User user, Member member) {
        return new ProfileResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().getName(),
            user.isActive(),
            member == null ? null : member.getId(),
            member == null ? null : member.getMemberType().getTypeName(),
            member == null ? null : member.getStatus()
        );
    }
}
