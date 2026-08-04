package com.finflow.auth.security;

import com.finflow.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.secret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(UUID userId) {

        Instant now = Instant.now();

        Instant expiresAt = now.plus(
                jwtProperties.accessTokenExpiration()
        );

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {

        Claims claims = parseClaims(token);

        return UUID.fromString(
                claims.getSubject()
        );
    }

    public boolean isTokenValid(String token) {

        try {
            parseClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties
                .accessTokenExpiration()
                .toSeconds();
    }

    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}