package com.kaoshi.exam.dto;

import java.math.BigDecimal;

public record ExamRuleResponse(
        Long id,
        Long bankId,
        String bankName,
        Integer singleCount,
        BigDecimal singleScore,
        Integer multipleCount,
        BigDecimal multipleScore,
        Integer writingCount,
        BigDecimal writingScore,
        Integer sortOrder
) {
}
