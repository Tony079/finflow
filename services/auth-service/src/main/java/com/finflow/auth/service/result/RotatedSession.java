package com.finflow.auth.service.result;

import java.util.UUID;

public record RotatedSession(
        UUID userId,
        String refreshToken
) {
}