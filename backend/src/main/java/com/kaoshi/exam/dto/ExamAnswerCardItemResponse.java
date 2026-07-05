package com.kaoshi.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamAnswerCardItemResponse(
        Long id,
        Integer questionNo,
        String answerType,
        List<String> optionLabels,
        List<String> correctLabels,
        BigDecimal score,
        Integer sortOrder
) {
}
