package com.kaoshi.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExamAnswerSnapshotRequest(
        @NotEmpty List<@Valid AnswerSubmitItem> answers
) {
}
