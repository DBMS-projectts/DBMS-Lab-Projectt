package com.lims.dto;

import jakarta.validation.constraints.NotBlank;

public record CopyConditionRequest(
    @NotBlank String conditionStatus
) {
}
