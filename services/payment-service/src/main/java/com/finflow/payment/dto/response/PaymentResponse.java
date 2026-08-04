package com.finflow.payment.dto.response;

import com.finflow.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}