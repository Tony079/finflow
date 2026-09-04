package com.finflow.fraud.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "INBOX_EVENT",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_INBOX_EVENT_EVENT_ID",
                        columnNames = "EVENT_ID"
                )
        }
)
public class InboxEvent {

    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Column(name = "EVENT_ID", nullable = false)
    private UUID eventId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    private String eventType;

    @Column(name = "AGGREGATE_TYPE", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "AGGREGATE_ID", nullable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private InboxStatus status;

    @Column(name = "RECEIVED_AT", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;

    protected InboxEvent() {
    }

    public InboxEvent(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId) {

        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.status = InboxStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.status = InboxStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = InboxStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = InboxStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public InboxStatus getStatus() {
        return status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}