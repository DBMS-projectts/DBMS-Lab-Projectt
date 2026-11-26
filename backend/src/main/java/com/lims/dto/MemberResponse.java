package com.lims.dto;

import java.time.LocalDate;

public record MemberResponse(
    Long memberId,
    Long userId,
    String fullName,
    String email,
    String phone,
    String memberType,
    LocalDate joinDate,
    LocalDate expiryDate,
    String status,
    long activeBorrowings
) {
}
