package com.kaoshi.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WritingReviewRequest(
        @NotNull @DecimalMin("0.00") BigDecimal score,
        @Size(max = 1000) String comment
) {
}
