package com.finflow.auth.service;

import com.finflow.auth.entity.AuthUser;
import com.finflow.auth.repository.AuthUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoginStateService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final AuthUserRepository authUserRepository;

    public LoginStateService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Transactional
    public void recordFailure(UUID userId, LocalDateTime now) {

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow();

        user.recordFailedLogin(
                now,
                MAX_FAILED_ATTEMPTS,
                LOCK_DURATION_MINUTES
        );
    }

    @Transactional
    public void recordSuccess(UUID userId, LocalDateTime now) {

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow();

        user.recordSuccessfulLogin(now);
    }

    @Transactional
    public void clearExpiredLock(UUID userId) {

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow();

        user.clearExpiredLock();
    }
}