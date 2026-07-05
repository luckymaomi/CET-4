package com.kaoshi.question.dto;

import java.util.List;

public record QuestionResponse(
        Long id,
        Long bankId,
        String bankName,
        String type,
        String stem,
        String analysis,
        String difficulty,
        String status,
        List<QuestionOptionResponse> options,
        List<QuestionAttachmentResponse> attachments
) {
}

