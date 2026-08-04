package com.finflow.payment.messaging;

import com.finflow.payment.config.RabbitMqConfig;
import com.finflow.payment.domain.OutboxStatus;
import com.finflow.payment.entity.OutboxEvent;
import com.finflow.payment.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate) {

        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:5000}")
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING
                        );

        for (OutboxEvent event : events) {

            CorrelationData correlationData =
                    new CorrelationData(
                            event.getId().toString()
                    );

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.PAYMENT_EXCHANGE,
                    resolveRoutingKey(event),
                    event.getPayload(),
                    correlationData
            );

            try {

                CorrelationData.Confirm confirm =
                        correlationData
                                .getFuture()
                                .get(5, TimeUnit.SECONDS);

                if (confirm.isAck()) {

                    event.markPublished(
                            LocalDateTime.now()
                    );

                } else {

                    System.err.println(
                            "RabbitMQ rejected event: "
                                    + event.getId()
                                    + ", reason: "
                                    + confirm.getReason()
                    );
                }

            } catch (Exception exception) {

                System.err.println(
                        "Failed to confirm RabbitMQ event: "
                                + event.getId()
                );
            }
        }
    }

    private String resolveRoutingKey(
            OutboxEvent event) {

        if ("PaymentCreated".equals(event.getEventType())) {
            return RabbitMqConfig.PAYMENT_CREATED_ROUTING_KEY;
        }

        throw new IllegalArgumentException(
                "Unsupported outbox event type: "
                        + event.getEventType()
        );
    }
}