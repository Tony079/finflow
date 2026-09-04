package com.finflow.fraud.service;

import com.finflow.fraud.domain.FraudDecision;
import com.finflow.fraud.domain.FraudResult;
import com.finflow.fraud.messaging.PaymentCreatedEvent;
import com.finflow.fraud.repository.FraudResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class FraudDetectionService {

    private static final BigDecimal REVIEW_THRESHOLD =
            new BigDecimal("100000");

    private static final BigDecimal REJECT_THRESHOLD =
            new BigDecimal("1000000");

    private final FraudResultRepository fraudResultRepository;

    public FraudDetectionService(
            FraudResultRepository fraudResultRepository) {

        this.fraudResultRepository = fraudResultRepository;
    }

    @Transactional
    public void process(PaymentCreatedEvent event) {

        if (fraudResultRepository.existsByEventId(event.eventId())) {
            System.out.println(
                    "Fraud result already exists for event: "
                            + event.eventId()
            );
            return;
        }

        FraudEvaluation evaluation =
                evaluate(event);

        FraudResult fraudResult =
                new FraudResult(
                        event.eventId(),
                        event.paymentId(),
                        evaluation.riskScore(),
                        evaluation.decision(),
                        evaluation.reason()
                );

        fraudResultRepository.save(fraudResult);

        System.out.println(
                "Fraud evaluation completed. "
                        + "paymentId=" + event.paymentId()
                        + ", decision=" + evaluation.decision()
                        + ", riskScore=" + evaluation.riskScore()
        );
    }

    private FraudEvaluation evaluate(
            PaymentCreatedEvent event) {

        BigDecimal amount = event.amount();

        if (amount.compareTo(REJECT_THRESHOLD) >= 0) {

            return new FraudEvaluation(
                    100,
                    FraudDecision.REJECTED,
                    "Payment amount exceeds rejection threshold"
            );
        }

        if (amount.compareTo(REVIEW_THRESHOLD) >= 0) {

            return new FraudEvaluation(
                    60,
                    FraudDecision.REVIEW,
                    "Payment amount requires manual review"
            );
        }

        return new FraudEvaluation(
                10,
                FraudDecision.APPROVED,
                "Payment amount is within normal threshold"
        );
    }

    private record FraudEvaluation(
            int riskScore,
            FraudDecision decision,
            String reason
    ) {
    }
}