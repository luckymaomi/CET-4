package com.kaoshi.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExamRuleRequest(
        @NotNull Long bankId,
        @NotNull @Min(0) Integer singleCount,
        @NotNull @DecimalMin("0.00") BigDecimal singleScore,
        @NotNull @Min(0) Integer multipleCount,
        @NotNull @DecimalMin("0.00") BigDecimal multipleScore,
        @Min(0) Integer writingCount,
        @DecimalMin("0.00") BigDecimal writingScore
) {
}
