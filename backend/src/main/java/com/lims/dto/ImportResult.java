package com.lims.dto;

import java.util.List;

public record ImportResult(
    int created,
    int skipped,
    List<String> errors
) {
}
