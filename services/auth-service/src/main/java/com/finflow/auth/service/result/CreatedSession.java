package com.finflow.auth.service.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreatedSession(
        UUID sessionId,
        String refreshToken,
        LocalDateTime expiresAt
) {
}