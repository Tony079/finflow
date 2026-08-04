package com.finflow.payment.controller;

import com.finflow.payment.dto.request.CreatePaymentRequest;
import com.finflow.payment.dto.response.PaymentResponse;
import com.finflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(
            @RequestHeader("User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        return paymentService.createPayment(
                userId,
                idempotencyKey,
                request
        );
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @RequestHeader("User-Id") UUID userId,
            @PathVariable UUID paymentId) {

        return paymentService.getPayment(
                userId,
                paymentId
        );
    }
}