package com.lims.dto;

public record CopyResponse(
    Long id,
    Long bookId,
    String bookTitle,
    String accessionNumber,
    String copyStatus,
    String conditionStatus
) {
}
