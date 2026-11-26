package com.lims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @Size(max = 25) String phone,
    @NotBlank String memberTypeName
) {
}
