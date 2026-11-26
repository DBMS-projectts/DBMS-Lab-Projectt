package com.lims.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MemberUpdateRequest(
    @NotBlank String memberTypeName,
    LocalDate expiryDate,
    @NotBlank String status
) {
}
