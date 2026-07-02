package com.kaoshi.question;

import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.common.excel.ExcelImportResult;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.question.dto.QuestionResponse;
import com.kaoshi.question.dto.QuestionSaveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/questions")
@PreAuthorize("hasAuthority('system:admin')")
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<QuestionResponse>> page(
            @RequestParam(required = false) Long bankId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(questionService.page(bankId, new PageRequest(page, size, keyword)));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(questionService.detail(id));
    }

    @PostMapping
    public ApiResponse<QuestionResponse> create(@Valid @RequestBody QuestionSaveRequest request) {
        return ApiResponse.ok(questionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuestionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionSaveRequest request
    ) {
        return ApiResponse.ok(questionService.update(id, request));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        return questionService.template();
    }

    @PostMapping("/import")
    public ApiResponse<ExcelImportResult> importExcel(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(questionService.importExcel(file));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        return questionService.exportExcel();
    }
}

