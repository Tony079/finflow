package com.finflow.auth.entity;

import com.finflow.auth.domain.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_user")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuthUser {

    private AuthUser(
            UUID id,
            String email,
            String passwordHash,
            UserStatus status,
            boolean emailVerified,
            int failedLoginAttempts) {

        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.emailVerified = emailVerified;
        this.failedLoginAttempts = failedLoginAttempts;
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AuthUser create(
            String email,
            String passwordHash) {

        return new AuthUser(
                UUID.randomUUID(),
                email,
                passwordHash,
                UserStatus.ACTIVE,
                false,
                0
        );
    }

    public boolean isDisabled() {
        return this.status == UserStatus.DISABLED;
    }

    public boolean isLockedAt(LocalDateTime now) {
        return this.status == UserStatus.LOCKED
                && this.accountLockedUntil != null
                && now.isBefore(this.accountLockedUntil);
    }

    public boolean hasExpiredLockAt(LocalDateTime now) {
        return this.status == UserStatus.LOCKED
                && this.accountLockedUntil != null
                && !now.isBefore(this.accountLockedUntil);
    }

    public void clearExpiredLock() {
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void recordFailedLogin(
            LocalDateTime now,
            int maxAttempts,
            long lockDurationMinutes) {

        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= maxAttempts) {
            this.status = UserStatus.LOCKED;
            this.accountLockedUntil =
                    now.plusMinutes(lockDurationMinutes);
        }
    }

    public void recordSuccessfulLogin(LocalDateTime now) {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
        this.status = UserStatus.ACTIVE;
        this.lastLoginAt = now;
    }
}