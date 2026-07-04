package com.kaoshi.question.seed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record QuestionSetResource(
        String code,
        String name,
        List<CategoryResource> categories,
        List<BankResource> banks,
        List<QuestionResource> questions,
        List<ExamResource> exams
) {
    record CategoryResource(String code, String name, String description, Integer sortOrder) {
    }

    record BankResource(String code, String categoryCode, String name, String description, String status) {
    }

    record QuestionResource(
            String code,
            String bankCode,
            String type,
            String stem,
            String analysis,
            String difficulty,
            String status,
            List<OptionResource> options,
            List<AttachmentResource> attachments
    ) {
    }

    record OptionResource(String label, String content, boolean correct, Integer sortOrder) {
    }

    record AttachmentResource(String fileName, String fileUrl, String mediaType, Integer sortOrder) {
    }

    record ExamResource(
            String code,
            String title,
            String description,
            BigDecimal qualifyScore,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            Boolean timeLimit,
            Integer attemptLimit,
            String displayMode,
            String questionOrderMode,
            String openType,
            String status,
            List<PaperQuestionResource> paperQuestions
    ) {
    }

    record PaperQuestionResource(String questionCode, BigDecimal score, Integer sortOrder) {
    }
}
