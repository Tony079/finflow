package com.finflow.fraud.repository;

import com.finflow.fraud.domain.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepository
        extends JpaRepository<InboxEvent, UUID> {

    boolean existsByEventId(UUID eventId);

    Optional<InboxEvent> findByEventId(UUID eventId);

    @Modifying
    @Query(value = """
            INSERT INTO inbox_event (
                id,
                event_id,
                event_type,
                aggregate_type,
                aggregate_id,
                status,
                received_at
            )
            VALUES (
                :id,
                :eventId,
                :eventType,
                :aggregateType,
                :aggregateId,
                'RECEIVED',
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfNotExists(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") UUID aggregateId
    );
}