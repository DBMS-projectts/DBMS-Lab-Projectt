package com.lims.dto;

import jakarta.validation.constraints.NotBlank;

public record CopyStatusRequest(
    @NotBlank String status
) {
}
