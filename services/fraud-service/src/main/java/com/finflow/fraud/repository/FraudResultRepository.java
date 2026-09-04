package com.finflow.fraud.repository;

import com.finflow.fraud.domain.FraudResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FraudResultRepository
        extends JpaRepository<FraudResult, UUID> {

    Optional<FraudResult> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}