package com.kaoshi.exam;

import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.exam.dto.ExamResponse;
import com.kaoshi.exam.dto.ExamSaveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/exams")
@PreAuthorize("hasAuthority('system:admin')")
public class AdminExamController {
    private final ExamService examService;

    public AdminExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ExamResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(examService.page(new PageRequest(page, size, keyword)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(examService.detail(id));
    }

    @PostMapping
    public ApiResponse<ExamResponse> create(@Valid @RequestBody ExamSaveRequest request) {
        return ApiResponse.ok(examService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExamResponse> update(@PathVariable Long id, @Valid @RequestBody ExamSaveRequest request) {
        return ApiResponse.ok(examService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ExamResponse> publish(@PathVariable Long id) {
        return ApiResponse.ok(examService.publish(id));
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<ExamResponse> copy(@PathVariable Long id) {
        return ApiResponse.ok(examService.copy(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return examService.download(id);
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<ExamResponse> revoke(@PathVariable Long id) {
        return ApiResponse.ok(examService.revoke(id));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ExamResponse> close(@PathVariable Long id) {
        return ApiResponse.ok(examService.close(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return ApiResponse.ok(null);
    }
}

