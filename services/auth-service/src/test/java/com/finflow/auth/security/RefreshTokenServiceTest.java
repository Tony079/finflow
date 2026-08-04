package com.finflow.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenServiceTest {

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService();
    }

    @Test
    void shouldGenerateNonEmptyToken() {

        String token = refreshTokenService.generateToken();

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldGenerateDifferentTokens() {

        String firstToken = refreshTokenService.generateToken();
        String secondToken = refreshTokenService.generateToken();

        assertNotEquals(firstToken, secondToken);
    }

    @Test
    void shouldGenerateUrlSafeToken() {

        String token = refreshTokenService.generateToken();

        assertTrue(token.matches("^[A-Za-z0-9_-]+$"));
    }

    @Test
    void shouldHashTokenDeterministically() {

        String rawToken = "test-refresh-token";

        String firstHash = refreshTokenService.hashToken(rawToken);
        String secondHash = refreshTokenService.hashToken(rawToken);

        assertEquals(firstHash, secondHash);
    }

    @Test
    void shouldProduceDifferentHashesForDifferentTokens() {

        String firstHash =
                refreshTokenService.hashToken("token-one");

        String secondHash =
                refreshTokenService.hashToken("token-two");

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void shouldProduceSha256HexHashWith64Characters() {

        String hash =
                refreshTokenService.hashToken("test-refresh-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[0-9a-f]{64}$"));
    }
}