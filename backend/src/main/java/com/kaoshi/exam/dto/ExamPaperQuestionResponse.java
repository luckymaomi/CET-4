package com.kaoshi.exam.dto;

import java.math.BigDecimal;

public record ExamPaperQuestionResponse(
        Long questionId,
        Long bankId,
        String bankName,
        String type,
        String stem,
        BigDecimal score,
        Integer sortOrder
) {
}
