package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^(PENDING|MATCHED|UNMATCHED|DISPUTED|CANCELLED)$",
                message = "status must be one of: PENDING, MATCHED, UNMATCHED, DISPUTED, CANCELLED")
        String status
) {}
