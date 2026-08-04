package com.finflow.fraud.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCreatedEvent(
        UUID eventId,
        UUID paymentId,
        UUID userId,
        BigDecimal amount,
        String currency,
        LocalDateTime occurredAt
) {
}