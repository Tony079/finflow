package com.finflow.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.payment.domain.PaymentStatus;
import com.finflow.payment.dto.request.CreatePaymentRequest;
import com.finflow.payment.dto.response.PaymentResponse;
import com.finflow.payment.entity.Payment;
import com.finflow.payment.exception.PaymentNotFoundException;
import com.finflow.payment.repository.OutboxEventRepository;
import com.finflow.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        paymentService = new PaymentService(
                paymentRepository,
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void shouldCreateNewPaymentWhenIdempotencyKeyDoesNotExist() {

        UUID userId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        new BigDecimal("1000.00"),
                        "usd",
                        "Loan repayment"
                );

        when(paymentRepository.findByUserIdAndIdempotencyKey(
                userId,
                "idem-123"))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.createPayment(
                        userId,
                        "idem-123",
                        request
                );

        assertEquals(
                new BigDecimal("1000.00"),
                response.amount()
        );

        assertEquals(
                "USD",
                response.currency()
        );

        assertEquals(
                PaymentStatus.PENDING,
                response.status()
        );

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldReturnExistingPaymentWhenIdempotencyKeyAlreadyExists() {

        UUID userId = UUID.randomUUID();

        Payment existingPayment =
                Payment.create(
                        userId,
                        "idem-123",
                        new BigDecimal("500"),
                        "INR",
                        "Existing payment",
                        LocalDateTime.now()
                );

        when(paymentRepository.findByUserIdAndIdempotencyKey(
                userId,
                "idem-123"))
                .thenReturn(Optional.of(existingPayment));

        PaymentResponse response =
                paymentService.createPayment(
                        userId,
                        "idem-123",
                        new CreatePaymentRequest(
                                new BigDecimal("999"),
                                "USD",
                                "Should not create"
                        )
                );

        assertEquals(
                existingPayment.getId(),
                response.paymentId()
        );

        verify(paymentRepository, never())
                .save(any());
        verify(paymentRepository, never())
                .save(any());

        verify(outboxEventRepository, never())
                .save(any());
    }

    @Test
    void shouldNormalizeCurrencyToUpperCase() {

        UUID userId = UUID.randomUUID();

        when(paymentRepository.findByUserIdAndIdempotencyKey(
                any(),
                any()))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createPayment(
                userId,
                "idem-123",
                new CreatePaymentRequest(
                        new BigDecimal("50"),
                        "usd",
                        "Coffee"
                )
        );

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(captor.capture());

        assertEquals(
                "USD",
                captor.getValue().getCurrency()
        );
    }

    @Test
    void shouldStoreCorrectIdempotencyKey() {

        UUID userId = UUID.randomUUID();

        when(paymentRepository.findByUserIdAndIdempotencyKey(
                any(),
                any()))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createPayment(
                userId,
                "payment-001",
                new CreatePaymentRequest(
                        new BigDecimal("200"),
                        "INR",
                        "Test"
                )
        );

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(captor.capture());

        assertEquals(
                "payment-001",
                captor.getValue().getIdempotencyKey()
        );
    }

    @Test
    void shouldReturnExistingPaymentWhenConcurrentInsertOccurs() {

        UUID userId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        new BigDecimal("100"),
                        "INR",
                        "Race Condition"
                );

        Payment existing =
                Payment.create(
                        userId,
                        "idem-1",
                        new BigDecimal("100"),
                        "INR",
                        "Race Condition",
                        LocalDateTime.now()
                );

        when(paymentRepository.findByUserIdAndIdempotencyKey(
                userId,
                "idem-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(DataIntegrityViolationException.class);

        PaymentResponse response =
                paymentService.createPayment(
                        userId,
                        "idem-1",
                        request
                );

        assertEquals(
                existing.getId(),
                response.paymentId()
        );
    }

    @Test
    void shouldReturnPaymentWhenPaymentBelongsToUser() {

        UUID userId = UUID.randomUUID();

        Payment payment = Payment.create(
                userId,
                "idem-123",
                new BigDecimal("1000.00"),
                "INR",
                "Test payment",
                LocalDateTime.now()
        );

        when(paymentRepository.findById(payment.getId()))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.getPayment(
                        userId,
                        payment.getId()
                );

        assertEquals(payment.getId(), response.paymentId());
        assertEquals(userId, response.userId());
        assertEquals(PaymentStatus.PENDING, response.status());

        verify(paymentRepository)
                .findById(payment.getId());
    }

    @Test
    void shouldThrowPaymentNotFoundWhenPaymentDoesNotExist() {

        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.getPayment(
                        userId,
                        paymentId
                )
        );
    }

    @Test
    void shouldThrowPaymentNotFoundWhenPaymentBelongsToAnotherUser() {

        UUID ownerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();

        Payment payment = Payment.create(
                ownerId,
                "idem-123",
                new BigDecimal("500.00"),
                "INR",
                "Private payment",
                LocalDateTime.now()
        );

        when(paymentRepository.findById(payment.getId()))
                .thenReturn(Optional.of(payment));

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.getPayment(
                        anotherUserId,
                        payment.getId()
                )
        );
    }
}