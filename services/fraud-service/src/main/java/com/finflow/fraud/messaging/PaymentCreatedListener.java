package com.finflow.fraud.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.fraud.config.RabbitMqConfig;
import com.finflow.fraud.service.FraudDetectionService;
import com.finflow.fraud.service.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreatedListener {

    private final ObjectMapper objectMapper;
    private final FraudDetectionService fraudDetectionService;
    private final InboxService inboxService;

    public PaymentCreatedListener(
            ObjectMapper objectMapper,
            FraudDetectionService fraudDetectionService,
            InboxService inboxService) {

        this.objectMapper = objectMapper;
        this.fraudDetectionService = fraudDetectionService;
        this.inboxService = inboxService;
    }

    @RabbitListener(
            queues = RabbitMqConfig.PAYMENT_CREATED_QUEUE
    )
    public void consume(String payload) throws Exception {

        PaymentCreatedEvent event =
                objectMapper.readValue(
                        payload,
                        PaymentCreatedEvent.class
                );

        boolean newEvent = inboxService.createInboxEvent(
                event.eventId(),
                "PaymentCreated",
                "Payment",
                event.paymentId()
        );

        if (!newEvent) {
            return;
        }

        inboxService.markProcessing(event.eventId());

        fraudDetectionService.process(event);

        inboxService.markCompleted(event.eventId());
    }
}