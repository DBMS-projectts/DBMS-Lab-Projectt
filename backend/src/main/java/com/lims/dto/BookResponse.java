package com.lims.dto;

public record BookResponse(
    Long id,
    String isbn,
    String title,
    String edition,
    Integer publicationYear,
    Long categoryId,
    String categoryName,
    Long publisherId,
    String publisherName
) {
}
