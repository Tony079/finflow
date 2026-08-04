package com.finflow.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.payment.dto.request.CreatePaymentRequest;
import com.finflow.payment.dto.response.PaymentResponse;
import com.finflow.payment.entity.OutboxEvent;
import com.finflow.payment.entity.Payment;
import com.finflow.payment.event.PaymentCreatedEvent;
import com.finflow.payment.exception.PaymentNotFoundException;
import com.finflow.payment.repository.OutboxEventRepository;
import com.finflow.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {

        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse createPayment(
            UUID userId,
            String idempotencyKey,
            CreatePaymentRequest request) {

        Payment existing = paymentRepository
                .findByUserIdAndIdempotencyKey(
                        userId,
                        idempotencyKey
                )
                .orElse(null);

        if (existing != null) {
            return toResponse(existing);
        }

        try {
            return createNewPayment(
                    userId,
                    idempotencyKey,
                    request
            );
        } catch (DataIntegrityViolationException ex) {

            Payment payment = paymentRepository
                    .findByUserIdAndIdempotencyKey(
                            userId,
                            idempotencyKey
                    )
                    .orElseThrow(() -> ex);

            return toResponse(payment);
        }
    }

    private PaymentResponse createNewPayment(
            UUID userId,
            String idempotencyKey,
            CreatePaymentRequest request) {

        LocalDateTime now = LocalDateTime.now();

        String normalizedCurrency =
                request.currency()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        Payment payment = Payment.create(
                userId,
                idempotencyKey,
                request.amount(),
                normalizedCurrency,
                request.description(),
                now
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        createPaymentCreatedOutboxEvent(
                savedPayment,
                now
        );

        return toResponse(savedPayment);
    }

    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getDescription(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID userId,
            UUID paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .filter(existing ->
                        existing.getUserId().equals(userId))
                .orElseThrow(PaymentNotFoundException::new);

        return toResponse(payment);
    }

    private void createPaymentCreatedOutboxEvent(
            Payment payment,
            LocalDateTime now) {

        UUID eventId = UUID.randomUUID();

        PaymentCreatedEvent event =
                new PaymentCreatedEvent(
                        eventId,
                        payment.getId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        now
                );

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.create(
                            "PAYMENT",
                            payment.getId(),
                            "PaymentCreated",
                            payload,
                            now
                    );

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to serialize PaymentCreated event",
                    exception
            );
        }
    }
}