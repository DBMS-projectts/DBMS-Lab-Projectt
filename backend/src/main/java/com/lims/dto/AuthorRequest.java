package com.lims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorRequest(
    @NotBlank @Size(max = 150) String name,
    @Size(max = 80) String country
) {
}
