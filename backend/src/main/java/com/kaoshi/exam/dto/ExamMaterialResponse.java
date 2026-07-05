package com.kaoshi.exam.dto;

public record ExamMaterialResponse(
        Long id,
        String title,
        String description,
        String fileName,
        String fileUrl,
        String mediaType,
        Integer sortOrder
) {
}
