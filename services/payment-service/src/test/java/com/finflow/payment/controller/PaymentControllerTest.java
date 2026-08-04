package com.finflow.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.payment.domain.PaymentStatus;
import com.finflow.payment.dto.request.CreatePaymentRequest;
import com.finflow.payment.dto.response.PaymentResponse;
import com.finflow.payment.exception.GlobalExceptionHandler;
import com.finflow.payment.exception.PaymentNotFoundException;
import com.finflow.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentSuccessfully() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        new BigDecimal("1000.00"),
                        "INR",
                        "Loan repayment"
                );

        PaymentResponse response =
                new PaymentResponse(
                        paymentId,
                        userId,
                        new BigDecimal("1000.00"),
                        "INR",
                        PaymentStatus.PENDING,
                        "Loan repayment",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(paymentService.createPayment(
                eq(userId),
                eq("payment-001"),
                any(CreatePaymentRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header("User-Id", userId)
                                .header("Idempotency-Key", "payment-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId")
                        .value(paymentId.toString()))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.currency")
                        .value("INR"));
    }

    @Test
    void shouldReturnBadRequestWhenPaymentRequestIsInvalid()
            throws Exception {

        String request = """
                {
                    "amount": 0,
                    "currency": "IN"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "User-Id",
                                        UUID.randomUUID()
                                )
                                .header(
                                        "Idempotency-Key",
                                        "payment-001"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath(
                        "$.validationErrors.amount"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.currency"
                ).exists());
    }

    @Test
    void shouldGetPaymentSuccessfully() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentResponse response =
                new PaymentResponse(
                        paymentId,
                        userId,
                        new BigDecimal("500.00"),
                        "INR",
                        PaymentStatus.PENDING,
                        "Test payment",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(paymentService.getPayment(
                userId,
                paymentId
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}", paymentId)
                                .header("User-Id", userId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId")
                        .value(paymentId.toString()))
                .andExpect(jsonPath("$.amount")
                        .value(500.00))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundWhenPaymentDoesNotExist()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentService.getPayment(
                userId,
                paymentId
        )).thenThrow(
                new PaymentNotFoundException()
        );

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}", paymentId)
                                .header("User-Id", userId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Payment not found"));
    }
}