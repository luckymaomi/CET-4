package com.kaoshi.admin.user;

import com.kaoshi.admin.user.dto.AdminUserResponse;
import com.kaoshi.admin.user.dto.UserCreateRequest;
import com.kaoshi.admin.user.dto.UserStatusRequest;
import com.kaoshi.admin.user.dto.UserUpdateRequest;
import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.common.excel.ExcelImportResult;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('system:admin')")
public class AdminUserController {
    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(userService.page(new PageRequest(page, size, keyword)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(userService.detail(id));
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return ApiResponse.ok(userService.changeStatus(id, request.status()));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        return userService.template();
    }

    @PostMapping("/import")
    public ApiResponse<ExcelImportResult> importExcel(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(userService.importExcel(file));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        return userService.exportExcel();
    }
}

