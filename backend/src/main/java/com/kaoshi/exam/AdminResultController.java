package com.kaoshi.exam;

import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.exam.dto.ExamResultDetailResponse;
import com.kaoshi.exam.dto.ExamResultResponse;
import com.kaoshi.exam.dto.WritingReviewRequest;
import com.kaoshi.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/results")
@PreAuthorize("hasAuthority('system:admin')")
public class AdminResultController {
    private final ExamService examService;

    public AdminResultController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<List<ExamResultResponse>> list(@RequestParam(required = false) Long examId) {
        return ApiResponse.ok(examService.adminResults(examId));
    }

    @GetMapping("/{resultId}")
    public ApiResponse<ExamResultDetailResponse> detail(@PathVariable Long resultId) {
        return ApiResponse.ok(examService.adminResultDetail(resultId));
    }

    @PostMapping("/{resultId}/questions/{questionId}/review")
    public ApiResponse<ExamResultDetailResponse> reviewWriting(
            @PathVariable Long resultId,
            @PathVariable Long questionId,
            @Valid @RequestBody WritingReviewRequest request,
            @AuthenticationPrincipal AuthUser user
    ) {
        return ApiResponse.ok(examService.reviewWriting(resultId, questionId, user.id(), request));
    }

    @PostMapping("/{resultId}/complete-review")
    public ApiResponse<ExamResultDetailResponse> completeReview(
            @PathVariable Long resultId,
            @AuthenticationPrincipal AuthUser user
    ) {
        return ApiResponse.ok(examService.completeReview(resultId, user.id()));
    }
}

