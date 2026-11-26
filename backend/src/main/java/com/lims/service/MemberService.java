package com.lims.service;

import com.lims.dto.ImportResult;
import com.lims.dto.MemberCreateRequest;
import com.lims.dto.MemberResponse;
import com.lims.dto.MemberUpdateRequest;
import com.lims.entity.Member;
import com.lims.entity.MemberType;
import com.lims.entity.Role;
import com.lims.entity.User;
import com.lims.repository.MemberRepository;
import com.lims.repository.MemberTypeRepository;
import com.lims.repository.RoleRepository;
import com.lims.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class MemberService {
    /** Default password assigned by CSV bulk import; members must change it after first login. */
    public static final String IMPORT_DEFAULT_PASSWORD = "lims12345";

    private static final Set<String> MEMBERSHIP_STATUSES = Set.of("ACTIVE", "EXPIRED", "SUSPENDED");
    private static final Set<String> ROLE_PER_TYPE = Set.of("STUDENT", "TEACHER", "GENERAL");

    private final UserRepository users;
    private final RoleRepository roles;
    private final MemberTypeRepository memberTypes;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public MemberService(UserRepository users, RoleRepository roles,
                         MemberTypeRepository memberTypes, MemberRepository members,
                         PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.users = users;
        this.roles = roles;
        this.memberTypes = memberTypes;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    public MemberResponse create(MemberCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        MemberType memberType = getMemberType(request.memberTypeName());
        User user = newUser(request.fullName(), email, request.password(), request.phone(),
            roleFor(memberType.getTypeName()));
        Member member = newMember(user, memberType, LocalDate.now());
        return toResponse(member, activeBorrowingCounts());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list() {
        Map<Long, Long> counts = activeBorrowingCounts();
        return members.findAll().stream().map(member -> toResponse(member, counts)).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse get(Long id) {
        return toResponse(getMember(id), activeBorrowingCounts());
    }

    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = getMember(id);
        String status = request.status().trim().toUpperCase();
        if (!MEMBERSHIP_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown membership status: " + status);
        }
        member.setMemberType(getMemberType(request.memberTypeName()));
        member.setExpiryDate(request.expiryDate());
        member.setStatus(status);
        member = members.save(member);
        return toResponse(member, activeBorrowingCounts());
    }

    /** Soft delete: membership suspended and the login disabled; history is kept. */
    public void deactivate(Long id) {
        Member member = getMember(id);
        member.setStatus("SUSPENDED");
        members.save(member);
        User user = member.getUser();
        user.setActive(false);
        users.save(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(Long memberId) {
        getMember(memberId);
        return borrowingHistory(memberId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myHistory(String email) {
        User user = users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
        Member member = members.findByUser(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No library membership is linked to this account"));
        return borrowingHistory(member.getId());
    }

    /**
     * Bulk registration from admission data. Expected CSV header:
     * full_name,email,phone,member_type[,join_date]. Duplicate emails and
     * unknown member types are skipped and reported, never duplicated.
     */
    public ImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is required");
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
        if (lines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
        }
        Map<String, Integer> columns = headerIndex(lines.get(0));
        for (String required : List.of("full_name", "email", "member_type")) {
            if (!columns.containsKey(required)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CSV header must include column: " + required);
            }
        }

        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            int lineNumber = i + 1;
            String[] cells = raw.split(",", -1);
            String fullName = cell(cells, columns.get("full_name"));
            String email = cell(cells, columns.get("email"));
            String phone = columns.containsKey("phone") ? cell(cells, columns.get("phone")) : null;
            String typeName = cell(cells, columns.get("member_type"));
            String joinDateText = columns.containsKey("join_date") ? cell(cells, columns.get("join_date")) : null;

            if (fullName == null || email == null || typeName == null) {
                skipped++;
                addError(errors, "Line " + lineNumber + ": full_name, email and member_type are required");
                continue;
            }
            String normalizedEmail = email.toLowerCase();
            if (!seenInFile.add(normalizedEmail) || users.existsByEmail(normalizedEmail)) {
                skipped++;
                addError(errors, "Line " + lineNumber + ": duplicate email " + normalizedEmail + " skipped");
                continue;
            }
            MemberType memberType;
            try {
                memberType = getMemberType(typeName);
            } catch (ResponseStatusException ex) {
                skipped++;
                addError(errors, "Line " + lineNumber + ": " + ex.getReason());
                continue;
            }
            LocalDate joinDate = LocalDate.now();
            if (joinDateText != null) {
                try {
                    joinDate = LocalDate.parse(joinDateText);
                } catch (DateTimeParseException ex) {
                    skipped++;
                    addError(errors, "Line " + lineNumber + ": join_date must be YYYY-MM-DD");
                    continue;
                }
            }
            User user = newUser(fullName, normalizedEmail, IMPORT_DEFAULT_PASSWORD, phone,
                roleFor(memberType.getTypeName()));
            newMember(user, memberType, joinDate);
            created++;
        }
        return new ImportResult(created, skipped, errors);
    }

    /**
     * Library clearance (from the librarian interview): a member is cleared
     * only when every book is returned and every fine is settled. Clearance
     * ends the membership and disables the login.
     */
    public Map<String, Object> clearance(Long memberId) {
        Member member = getMember(memberId);
        Long active = jdbc.queryForObject(
            "SELECT COUNT(*) FROM borrowings WHERE member_id = ? AND return_date IS NULL",
            Long.class, memberId);
        BigDecimal unpaid = jdbc.queryForObject("""
            SELECT COALESCE(SUM(fb.balance_due), 0)
            FROM vw_fine_balances fb
            JOIN borrowings br ON br.borrowing_id = fb.borrowing_id
            WHERE br.member_id = ? AND fb.fine_status IN ('UNPAID','PARTIALLY_PAID')
            """, BigDecimal.class, memberId);
        long activeCount = active == null ? 0 : active;
        BigDecimal unpaidBalance = unpaid == null ? BigDecimal.ZERO : unpaid;
        if (activeCount > 0 || unpaidBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Clearance blocked: " + activeCount + " book(s) still on loan, unpaid fine balance "
                    + unpaidBalance.toPlainString());
        }
        member.setStatus("EXPIRED");
        members.save(member);
        User user = member.getUser();
        user.setActive(false);
        users.save(user);
        return Map.of(
            "memberId", memberId,
            "cleared", true,
            "message", "Clearance granted: membership closed and login deactivated");
    }

    private List<Map<String, Object>> borrowingHistory(Long memberId) {
        return jdbc.queryForList("""
            SELECT br.borrowing_id, b.title, bc.accession_number,
                   br.issue_date, br.due_date, br.return_date, br.renewal_count,
                   br.borrowing_status, f.fine_amount, f.fine_status
            FROM borrowings br
            JOIN book_copies bc ON bc.copy_id = br.copy_id
            JOIN books b ON b.book_id = bc.book_id
            LEFT JOIN fines f ON f.borrowing_id = br.borrowing_id
            WHERE br.member_id = ?
            ORDER BY br.issue_date DESC, br.borrowing_id DESC
            """, memberId);
    }

    private Map<Long, Long> activeBorrowingCounts() {
        Map<Long, Long> counts = new HashMap<>();
        jdbc.queryForList(
                "SELECT member_id, COUNT(*) AS active_count FROM borrowings "
                    + "WHERE return_date IS NULL GROUP BY member_id")
            .forEach(row -> counts.put(
                ((Number) row.get("member_id")).longValue(),
                ((Number) row.get("active_count")).longValue()));
        return counts;
    }

    private User newUser(String fullName, String email, String rawPassword, String phone, Role role) {
        User user = new User();
        user.setRole(role);
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setActive(true);
        return users.save(user);
    }

    private Member newMember(User user, MemberType memberType, LocalDate joinDate) {
        Member member = new Member();
        member.setUser(user);
        member.setMemberType(memberType);
        member.setJoinDate(joinDate);
        member.setStatus("ACTIVE");
        return members.save(member);
    }

    private Role roleFor(String memberTypeName) {
        if (ROLE_PER_TYPE.contains(memberTypeName)) {
            return roles.findByName(memberTypeName)
                .orElseGet(() -> roles.findByName("MEMBER")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Role MEMBER is not configured")));
        }
        return roles.findByName("MEMBER")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Role MEMBER is not configured"));
    }

    private MemberType getMemberType(String typeName) {
        String normalized = typeName == null ? "" : typeName.trim().toUpperCase();
        return memberTypes.findByTypeName(normalized)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown member type: " + typeName));
    }

    private Member getMember(Long id) {
        return members.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
    }

    private MemberResponse toResponse(Member member, Map<Long, Long> activeCounts) {
        User user = member.getUser();
        return new MemberResponse(
            member.getId(),
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            member.getMemberType().getTypeName(),
            member.getJoinDate(),
            member.getExpiryDate(),
            member.getStatus(),
            activeCounts.getOrDefault(member.getId(), 0L)
        );
    }

    private static Map<String, Integer> headerIndex(String headerLine) {
        Map<String, Integer> index = new HashMap<>();
        String[] cells = headerLine.split(",", -1);
        for (int i = 0; i < cells.length; i++) {
            index.put(cells[i].trim().toLowerCase(), i);
        }
        return index;
    }

    private static String cell(String[] cells, int index) {
        if (index < 0 || index >= cells.length) {
            return null;
        }
        String value = cells[index].trim();
        return value.isEmpty() ? null : value;
    }

    private static void addError(List<String> errors, String message) {
        if (errors.size() < 100) {
            errors.add(message);
        }
    }
}
