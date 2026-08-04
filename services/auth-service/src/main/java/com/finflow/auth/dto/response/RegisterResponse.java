package com.finflow.auth.dto.response;

import com.finflow.auth.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String email,
        UserStatus status,
        boolean emailVerified,
        LocalDateTime createdAt
) {
}