package com.finflow.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        @Size(
                min = 3,
                max = 3,
                message = "Currency must contain exactly 3 characters"
        )
        String currency,

        @Size(
                max = 255,
                message = "Description must not exceed 255 characters"
        )
        String description

) {
}