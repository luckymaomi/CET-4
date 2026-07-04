package com.kaoshi.exam.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AnswerSubmitItem(
        @NotNull Long questionId,
        List<String> selectedLabels,
        String answerText
) {
}

