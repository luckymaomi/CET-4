package com.kaoshi.auth;

import com.kaoshi.auth.dto.CurrentUserResponse;
import com.kaoshi.auth.dto.ChangePasswordRequest;
import com.kaoshi.auth.dto.LoginRequest;
import com.kaoshi.auth.dto.LoginResponse;
import com.kaoshi.common.api.ApiResponse;
import com.kaoshi.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(authService.toCurrentUser(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(user, request);
        return ApiResponse.ok();
    }
}

