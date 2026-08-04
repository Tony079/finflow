package com.finflow.auth.service;

import com.finflow.auth.config.JwtProperties;
import com.finflow.auth.entity.AuthSession;
import com.finflow.auth.repository.AuthSessionRepository;
import com.finflow.auth.security.RefreshTokenService;
import com.finflow.auth.service.result.CreatedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {

        JwtProperties jwtProperties = new JwtProperties(
                "ThisIsATestJwtSecretKeyThatIsAtLeast32BytesLong123456",
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );

        authSessionService = new AuthSessionService(
                authSessionRepository,
                refreshTokenService,
                jwtProperties
        );
    }

    @Test
    void shouldCreateSessionAndReturnRawRefreshToken() {

        UUID userId = UUID.randomUUID();

        when(refreshTokenService.generateToken())
                .thenReturn("raw-refresh-token");

        when(refreshTokenService.hashToken("raw-refresh-token"))
                .thenReturn("hashed-refresh-token");

        when(authSessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatedSession result =
                authSessionService.createSession(userId);

        assertNotNull(result.sessionId());
        assertEquals(
                "raw-refresh-token",
                result.refreshToken()
        );

        assertNotNull(result.expiresAt());

        verify(authSessionRepository)
                .save(any(AuthSession.class));
    }

    @Test
    void shouldStoreTokenHashInsteadOfRawRefreshToken() {

        UUID userId = UUID.randomUUID();

        when(refreshTokenService.generateToken())
                .thenReturn("raw-refresh-token");

        when(refreshTokenService.hashToken("raw-refresh-token"))
                .thenReturn("hashed-refresh-token");

        when(authSessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authSessionService.createSession(userId);

        ArgumentCaptor<AuthSession> sessionCaptor =
                ArgumentCaptor.forClass(AuthSession.class);

        verify(authSessionRepository)
                .save(sessionCaptor.capture());

        AuthSession savedSession =
                sessionCaptor.getValue();

        assertEquals(
                "hashed-refresh-token",
                savedSession.getTokenHash()
        );

        assertNotEquals(
                "raw-refresh-token",
                savedSession.getTokenHash()
        );
    }

    @Test
    void shouldCreateSessionForCorrectUser() {

        UUID userId = UUID.randomUUID();

        when(refreshTokenService.generateToken())
                .thenReturn("raw-refresh-token");

        when(refreshTokenService.hashToken(anyString()))
                .thenReturn("hashed-refresh-token");

        when(authSessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authSessionService.createSession(userId);

        ArgumentCaptor<AuthSession> sessionCaptor =
                ArgumentCaptor.forClass(AuthSession.class);

        verify(authSessionRepository)
                .save(sessionCaptor.capture());

        assertEquals(
                userId,
                sessionCaptor.getValue().getUserId()
        );
    }

    @Test
    void shouldCreateExpirationUsingConfiguredDuration() {

        UUID userId = UUID.randomUUID();

        when(refreshTokenService.generateToken())
                .thenReturn("raw-refresh-token");

        when(refreshTokenService.hashToken(anyString()))
                .thenReturn("hashed-refresh-token");

        when(authSessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before =
                LocalDateTime.now().plusDays(30);

        CreatedSession result =
                authSessionService.createSession(userId);

        LocalDateTime after =
                LocalDateTime.now().plusDays(30);

        assertFalse(result.expiresAt().isBefore(before));
        assertFalse(result.expiresAt().isAfter(after));
    }

    @Test
    void shouldCreateDifferentTokenFamiliesForSeparateLogins() {

        UUID userId = UUID.randomUUID();

        when(refreshTokenService.generateToken())
                .thenReturn(
                        "refresh-token-one",
                        "refresh-token-two"
                );

        when(refreshTokenService.hashToken("refresh-token-one"))
                .thenReturn("hash-one");

        when(refreshTokenService.hashToken("refresh-token-two"))
                .thenReturn("hash-two");

        when(authSessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authSessionService.createSession(userId);
        authSessionService.createSession(userId);

        ArgumentCaptor<AuthSession> sessionCaptor =
                ArgumentCaptor.forClass(AuthSession.class);

        verify(authSessionRepository, times(2))
                .save(sessionCaptor.capture());

        AuthSession firstSession =
                sessionCaptor.getAllValues().get(0);

        AuthSession secondSession =
                sessionCaptor.getAllValues().get(1);

        assertNotEquals(
                firstSession.getId(),
                secondSession.getId()
        );

        assertNotEquals(
                firstSession.getTokenFamilyId(),
                secondSession.getTokenFamilyId()
        );
    }
}