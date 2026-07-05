package com.kaoshi.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamMaterialRequest(
        @NotBlank String title,
        String description,
        @NotBlank String fileName,
        @NotBlank String fileUrl,
        @NotBlank String mediaType,
        @NotNull Integer sortOrder
) {
}
