package com.finflow.auth.entity;

import com.finflow.auth.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuthUserTest {

    @Test
    void shouldIncrementFailedLoginAttempts() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LocalDateTime now = LocalDateTime.of(2026, 7, 8, 22, 0);

        user.recordFailedLogin(now, 5, 15);

        assertEquals(1, user.getFailedLoginAttempts());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getAccountLockedUntil());
    }

    @Test
    void shouldLockAccountOnFifthFailedAttempt() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LocalDateTime now = LocalDateTime.of(2026, 7, 8, 22, 0);

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(now, 5, 15);
        }

        assertEquals(5, user.getFailedLoginAttempts());
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertEquals(
                now.plusMinutes(15),
                user.getAccountLockedUntil()
        );
    }

    @Test
    void shouldDetectActiveLock() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LocalDateTime now = LocalDateTime.of(2026, 7, 8, 22, 0);

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(now, 5, 15);
        }

        assertTrue(user.isLockedAt(now.plusMinutes(10)));
        assertFalse(user.isLockedAt(now.plusMinutes(15)));
    }

    @Test
    void shouldClearExpiredLock() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LocalDateTime now = LocalDateTime.of(2026, 7, 8, 22, 0);

        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(now, 5, 15);
        }

        assertTrue(user.hasExpiredLockAt(now.plusMinutes(15)));

        user.clearExpiredLock();

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getAccountLockedUntil());
    }

    @Test
    void shouldResetSecurityStateAfterSuccessfulLogin() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LocalDateTime now = LocalDateTime.of(2026, 7, 8, 22, 0);

        user.recordFailedLogin(now, 5, 15);
        user.recordFailedLogin(now, 5, 15);

        LocalDateTime loginTime = now.plusMinutes(5);

        user.recordSuccessfulLogin(loginTime);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getAccountLockedUntil());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(loginTime, user.getLastLoginAt());
    }
}