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
    @Column(name = "ID")
    private UUID id;

    @Column(name = "EVENT_ID")
    private UUID eventId;

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Column(name = "AGGREGATE_TYPE")
    private String aggregateType;

    @Column(name = "AGGREGATE_ID")
    private UUID aggregateId;

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
        this.processedAt = LocalDateTime.now();
    }
}