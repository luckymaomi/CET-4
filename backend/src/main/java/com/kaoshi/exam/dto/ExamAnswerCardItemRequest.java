package com.kaoshi.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ExamAnswerCardItemRequest(
        @NotNull @Min(1) Integer questionNo,
        @NotBlank String answerType,
        List<String> optionLabels,
        List<String> correctLabels,
        @NotNull @DecimalMin("0.00") BigDecimal score,
        @NotNull Integer sortOrder
) {
}
