package com.finflow.fraud.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "FRAUD_RESULT",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_FRAUD_RESULT_EVENT_ID",
                        columnNames = "EVENT_ID"
                )
        }
)
public class FraudResult {

    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Column(name = "EVENT_ID", nullable = false)
    private UUID eventId;

    @Column(name = "PAYMENT_ID", nullable = false)
    private UUID paymentId;

    @Column(name = "RISK_SCORE", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "DECISION", nullable = false, length = 20)
    private FraudDecision decision;

    @Column(name = "REASON", length = 255)
    private String reason;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected FraudResult() {
    }

    public FraudResult(
            UUID eventId,
            UUID paymentId,
            Integer riskScore,
            FraudDecision decision,
            String reason) {

        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public FraudDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}