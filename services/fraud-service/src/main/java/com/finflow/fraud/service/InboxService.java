package com.finflow.fraud.service;

import com.finflow.fraud.domain.InboxEvent;
import com.finflow.fraud.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public boolean createInboxEvent(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId) {

        int inserted =
                inboxEventRepository.insertIfNotExists(
                        UUID.randomUUID(),
                        eventId,
                        eventType,
                        aggregateType,
                        aggregateId
                );

        if (inserted == 0) {

            System.out.println(
                    "Duplicate event ignored: " + eventId
            );

            return false;
        }

        return true;
    }

    @Transactional
    public void markProcessing(UUID eventId) {

        InboxEvent inboxEvent =
                inboxEventRepository
                        .findByEventId(eventId)
                        .orElseThrow();

        inboxEvent.markProcessing();
    }

    @Transactional
    public void markCompleted(UUID eventId) {

        InboxEvent inboxEvent =
                inboxEventRepository
                        .findByEventId(eventId)
                        .orElseThrow();

        inboxEvent.markCompleted();
    }

    @Transactional
    public void markFailed(UUID eventId) {

        InboxEvent inboxEvent =
                inboxEventRepository
                        .findByEventId(eventId)
                        .orElseThrow();

        inboxEvent.markFailed();
    }
}