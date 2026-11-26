package com.lims.dto;

public record BookSummaryResponse(
    Long id,
    String title,
    String isbn
) {
}
