package com.finflow.auth.service;

import com.finflow.auth.dto.request.LoginRequest;
import com.finflow.auth.dto.request.RegisterRequest;
import com.finflow.auth.dto.response.RegisterResponse;
import com.finflow.auth.entity.AuthUser;
import com.finflow.auth.exception.AccountDisabledException;
import com.finflow.auth.exception.AccountLockedException;
import com.finflow.auth.exception.EmailAlreadyExistsException;
import com.finflow.auth.exception.InvalidCredentialsException;
import com.finflow.auth.repository.AuthUserRepository;
import com.finflow.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import com.finflow.auth.dto.response.LoginResponse;
import com.finflow.auth.service.result.CreatedSession;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginStateService loginStateService;
    private final JwtService jwtService;
    private final AuthSessionService authSessionService;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            LoginStateService loginStateService,
            JwtService jwtService,
            AuthSessionService authSessionService) {

        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginStateService = loginStateService;
        this.jwtService = jwtService;
        this.authSessionService = authSessionService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        if (authUserRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    "An account already exists with this email"
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        AuthUser user = AuthUser.create(
                normalizedEmail,
                passwordHash
        );

        AuthUser savedUser =
                authUserRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getStatus(),
                savedUser.isEmailVerified(),
                savedUser.getCreatedAt()
        );
    }

    public LoginResponse authenticate(LoginRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        AuthUser user = authUserRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        LocalDateTime now = LocalDateTime.now();

        if (user.isDisabled()) {
            throw new AccountDisabledException();
        }

        if (user.isLockedAt(now)) {
            throw new AccountLockedException(
                    user.getAccountLockedUntil()
            );
        }

        if (user.hasExpiredLockAt(now)) {
            loginStateService.clearExpiredLock(user.getId());
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            loginStateService.recordFailure(user.getId(), now);

            throw new InvalidCredentialsException();
        }

        loginStateService.recordSuccess(user.getId(), now);

        String accessToken =
                jwtService.generateAccessToken(user.getId());

        CreatedSession session =
                authSessionService.createSession(user.getId());

        return new LoginResponse(
                accessToken,
                session.refreshToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    private String normalizeEmail(String email) {

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

}