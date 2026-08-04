package com.finflow.auth.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_session")
public class AuthSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "token_family_id", nullable = false, updatable = false)
    private UUID tokenFamilyId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuthSession() {
    }

    private AuthSession(
            UUID id,
            UUID userId,
            String tokenHash,
            UUID tokenFamilyId,
            LocalDateTime expiresAt,
            LocalDateTime createdAt) {

        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.tokenFamilyId = tokenFamilyId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static AuthSession create(
            UUID userId,
            String tokenHash,
            UUID tokenFamilyId,
            LocalDateTime expiresAt,
            LocalDateTime now) {

        return new AuthSession(
                UUID.randomUUID(),
                userId,
                tokenHash,
                tokenFamilyId,
                expiresAt,
                now
        );
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isActiveAt(LocalDateTime now) {
        return !isRevoked() && !isExpiredAt(now);
    }

    public void revoke(
            LocalDateTime revokedAt,
            UUID replacedBySessionId) {

        this.revokedAt = revokedAt;
        this.replacedBySessionId = replacedBySessionId;
    }

    public void markUsed(LocalDateTime now) {
        this.lastUsedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedBySessionId() {
        return replacedBySessionId;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}