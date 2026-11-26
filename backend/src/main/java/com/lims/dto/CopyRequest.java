package com.lims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CopyRequest(
    @NotNull Long bookId,
    @NotBlank @Size(max = 50) String accessionNumber,
    String conditionStatus
) {
}
