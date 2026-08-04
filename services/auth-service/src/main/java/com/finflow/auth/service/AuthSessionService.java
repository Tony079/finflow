package com.finflow.auth.service;

import com.finflow.auth.config.JwtProperties;
import com.finflow.auth.entity.AuthSession;
import com.finflow.auth.exception.InvalidRefreshTokenException;
import com.finflow.auth.repository.AuthSessionRepository;
import com.finflow.auth.security.RefreshTokenService;
import com.finflow.auth.service.result.CreatedSession;
import com.finflow.auth.service.result.RotatedSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;


    public AuthSessionService(
            AuthSessionRepository authSessionRepository,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties) {

        this.authSessionRepository = authSessionRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public CreatedSession createSession(UUID userId) {

        LocalDateTime now = LocalDateTime.now();

        String rawRefreshToken =
                refreshTokenService.generateToken();

        String tokenHash =
                refreshTokenService.hashToken(rawRefreshToken);

        UUID tokenFamilyId = UUID.randomUUID();

        LocalDateTime expiresAt = now.plus(
                jwtProperties.refreshTokenExpiration()
        );

        AuthSession session = AuthSession.create(
                userId,
                tokenHash,
                tokenFamilyId,
                expiresAt,
                now
        );

        authSessionRepository.save(session);

        return new CreatedSession(
                session.getId(),
                rawRefreshToken,
                expiresAt
        );
    }

    @Transactional
    public RotatedSession rotateSession(String rawRefreshToken) {

        LocalDateTime now = LocalDateTime.now();

        String tokenHash =
                refreshTokenService.hashToken(rawRefreshToken);

        AuthSession currentSession = authSessionRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!currentSession.isActiveAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        String newRawRefreshToken =
                refreshTokenService.generateToken();

        String newTokenHash =
                refreshTokenService.hashToken(newRawRefreshToken);

        LocalDateTime newExpiresAt = now.plus(
                jwtProperties.refreshTokenExpiration()
        );

        AuthSession newSession = AuthSession.create(
                currentSession.getUserId(),
                newTokenHash,
                currentSession.getTokenFamilyId(),
                newExpiresAt,
                now
        );

        authSessionRepository.save(newSession);

        currentSession.markUsed(now);

        currentSession.revoke(
                now,
                newSession.getId()
        );

        return new RotatedSession(
                currentSession.getUserId(),
                newRawRefreshToken
        );
    }
}