package com.finflow.payment.repository;

import com.finflow.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByUserIdAndIdempotencyKey(
            UUID userId,
            String idempotencyKey
    );
}