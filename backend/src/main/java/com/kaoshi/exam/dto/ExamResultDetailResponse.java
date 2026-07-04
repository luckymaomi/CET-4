package com.kaoshi.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExamResultDetailResponse(
        Long id,
        Long attemptId,
        Long examId,
        String examTitle,
        Long userId,
        String username,
        String userName,
        String departmentName,
        BigDecimal totalScore,
        BigDecimal obtainedScore,
        BigDecimal objectiveScore,
        BigDecimal subjectiveScore,
        Integer correctCount,
        Integer questionCount,
        String gradingStatus,
        Boolean passed,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        List<ExamResultQuestionResponse> questions
) {
}
