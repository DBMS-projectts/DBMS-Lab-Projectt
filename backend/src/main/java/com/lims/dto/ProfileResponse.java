package com.lims.dto;

public record ProfileResponse(
    Long userId,
    String fullName,
    String email,
    String phone,
    String role,
    boolean active,
    Long memberId,
    String memberType,
    String membershipStatus
) {
}
