package com.finflow.fraud.service;

import com.finflow.fraud.domain.InboxEvent;
import com.finflow.fraud.repository.InboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public void execute(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            Runnable businessLogic) {

        try {

            InboxEvent inboxEvent = new InboxEvent(
                    eventId,
                    eventType,
                    aggregateType,
                    aggregateId
            );

            inboxEventRepository.save(inboxEvent);

        } catch (DataIntegrityViolationException ex) {

            // Event already processed
            return;
        }

        businessLogic.run();
    }
}