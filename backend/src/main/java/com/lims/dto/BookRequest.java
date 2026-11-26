package com.lims.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookRequest(
    @NotBlank @Size(max = 20) String isbn,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 50) String edition,
    @Min(1400) @Max(2100) Integer publicationYear,
    @NotNull Long categoryId,
    Long publisherId,
    List<Long> authorIds,
    @Size(max = 500) String coverImageUrl
) {
    /** Backwards-compatible constructor without authors / cover image. */
    public BookRequest(String isbn, String title, String edition, Integer publicationYear,
                       Long categoryId, Long publisherId) {
        this(isbn, title, edition, publicationYear, categoryId, publisherId, null, null);
    }
}
