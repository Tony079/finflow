package com.finflow.auth.security;

import com.finflow.auth.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        JwtProperties properties = new JwtProperties(
                "ThisIsATestJwtSecretKeyThatIsAtLeast32BytesLong123456",
                Duration.ofMinutes(15),
                Duration.ofMinutes(30)
        );

        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateValidAccessToken() {

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void shouldExtractUserIdFromToken() {

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId);

        UUID extractedUserId =
                jwtService.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void shouldRejectMalformedToken() {

        assertFalse(
                jwtService.isTokenValid("not-a-valid-jwt")
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {

        JwtProperties otherProperties = new JwtProperties(
                "AnotherTestJwtSecretKeyThatIsAtLeast32BytesLong123456",
                Duration.ofMinutes(15),
                Duration.ofMinutes(30)
        );

        JwtService otherJwtService =
                new JwtService(otherProperties);

        String token = jwtService.generateAccessToken(
                UUID.randomUUID()
        );

        assertFalse(
                otherJwtService.isTokenValid(token)
        );
    }

    @Test
    void shouldReturnConfiguredExpirationInSeconds() {

        assertEquals(
                900,
                jwtService.getAccessTokenExpirationSeconds()
        );
    }
}