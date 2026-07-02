package com.kaoshi.auth.dto;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        Boolean mustChangePassword,
        List<String> roles,
        List<String> permissions
) {
}

