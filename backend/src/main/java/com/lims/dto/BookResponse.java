package com.lims.dto;

import java.util.List;

public record BookResponse(
    Long id,
    String isbn,
    String title,
    String edition,
    Integer publicationYear,
    Long categoryId,
    String categoryName,
    Long publisherId,
    String publisherName,
    List<AuthorSummary> authors,
    String coverImageUrl
) {
    public record AuthorSummary(Long id, String name) {
    }
}
