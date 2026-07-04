package com.kaoshi.exam;

import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.exam.dto.ExamResultDetailResponse;
import com.kaoshi.exam.dto.ExamAnswerSnapshotRequest;
import com.kaoshi.exam.dto.ExamResponse;
import com.kaoshi.exam.dto.ExamResultResponse;
import com.kaoshi.exam.dto.ExamSessionResponse;
import com.kaoshi.exam.dto.ExamSubmitRequest;
import com.kaoshi.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam")
@PreAuthorize("hasAuthority('exam:take') or hasAuthority('system:admin')")
public class ExamPortalController {
    private final ExamService examService;

    public ExamPortalController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ExamResponse>> tasks(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(examService.publishedExams(user.id()));
    }

    @PostMapping("/{examId}/start")
    public ApiResponse<ExamSessionResponse> start(
            @PathVariable Long examId,
            @AuthenticationPrincipal AuthUser user
    ) {
        return ApiResponse.ok(examService.startExam(examId, user.id()));
    }

    @PostMapping("/{examId}/submit")
    public ApiResponse<ExamResultResponse> submit(
            @PathVariable Long examId,
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ExamSubmitRequest request
    ) {
        return ApiResponse.ok(examService.submit(examId, user.id(), request));
    }

    @PostMapping("/{examId}/answers")
    public ApiResponse<ExamSessionResponse> saveAnswer(
            @PathVariable Long examId,
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ExamAnswerSnapshotRequest request
    ) {
        return ApiResponse.ok(examService.saveAnswerSnapshot(examId, user.id(), request));
    }

    @GetMapping("/results")
    public ApiResponse<List<ExamResultResponse>> myResults(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(examService.userResults(user.id()));
    }

    @GetMapping("/results/{resultId}")
    public ApiResponse<ExamResultDetailResponse> myResultDetail(
            @PathVariable Long resultId,
            @AuthenticationPrincipal AuthUser user
    ) {
        return ApiResponse.ok(examService.userResultDetail(resultId, user.id()));
    }
}

