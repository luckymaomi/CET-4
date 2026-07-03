package com.kaoshi.exam;

import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.exam.dto.ExamResultDetailResponse;
import com.kaoshi.exam.dto.ExamResultResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}

