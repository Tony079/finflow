package com.finflow.auth.service;

import com.finflow.auth.domain.UserStatus;
import com.finflow.auth.dto.request.LoginRequest;
import com.finflow.auth.dto.request.RegisterRequest;
import com.finflow.auth.dto.response.LoginResponse;
import com.finflow.auth.dto.response.RegisterResponse;
import com.finflow.auth.entity.AuthUser;
import com.finflow.auth.exception.EmailAlreadyExistsException;
import com.finflow.auth.exception.InvalidCredentialsException;
import com.finflow.auth.repository.AuthUserRepository;
import com.finflow.auth.security.JwtService;
import com.finflow.auth.service.result.CreatedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @Mock
    private LoginStateService loginStateService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authUserRepository,
                passwordEncoder,
                loginStateService,
                jwtService,
                authSessionService
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "Tony@Example.com",
                "StrongPassword@123"
        );

        when(authUserRepository.existsByEmail("tony@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword@123"))
                .thenReturn("encoded-password");

        when(authUserRepository.save(any(AuthUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(request);

        assertNotNull(response.userId());
        assertEquals("tony@example.com", response.email());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertFalse(response.emailVerified());

        verify(authUserRepository)
                .existsByEmail("tony@example.com");

        verify(passwordEncoder)
                .encode("StrongPassword@123");

        verify(authUserRepository)
                .save(any(AuthUser.class));
    }

    @Test
    void shouldStoreEncodedPasswordInsteadOfRawPassword() {

        RegisterRequest request = new RegisterRequest(
                "tony@example.com",
                "RawPassword@123"
        );

        when(authUserRepository.existsByEmail("tony@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("RawPassword@123"))
                .thenReturn("encoded-password");

        when(authUserRepository.save(any(AuthUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<AuthUser> userCaptor =
                ArgumentCaptor.forClass(AuthUser.class);

        verify(authUserRepository).save(userCaptor.capture());

        AuthUser capturedUser = userCaptor.getValue();

        assertEquals("encoded-password", capturedUser.getPasswordHash());
        assertNotEquals("RawPassword@123", capturedUser.getPasswordHash());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest(
                "tony@example.com",
                "StrongPassword@123"
        );

        when(authUserRepository.existsByEmail("tony@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(authUserRepository, never()).save(any(AuthUser.class));
    }

    @Test
    void shouldNormalizeEmailBeforeCheckingDuplicate() {

        RegisterRequest request = new RegisterRequest(
                "  TONY@EXAMPLE.COM  ",
                "StrongPassword@123"
        );

        when(authUserRepository.existsByEmail("tony@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(authUserRepository)
                .existsByEmail("tony@example.com");
    }

    @Test
    void shouldThrowInvalidCredentialsWhenEmailDoesNotExist() {

        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "Password@123"
        );

        when(authUserRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(loginStateService);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRecordFailureWhenPasswordIsIncorrect() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LoginRequest request = new LoginRequest(
                "tony@example.com",
                "WrongPassword@123"
        );

        when(authUserRepository.findByEmail("tony@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword@123",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        verify(loginStateService)
                .recordFailure(eq(user.getId()), any(LocalDateTime.class));

        verify(loginStateService, never())
                .recordSuccess(any(), any());

        verify(jwtService, never())
                .generateAccessToken(any());
    }

    @Test
    void shouldRecordSuccessAndReturnAccessTokenWhenPasswordIsCorrect() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LoginRequest request = new LoginRequest(
                "tony@example.com",
                "StrongPassword@123"
        );

        when(authUserRepository.findByEmail("tony@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "StrongPassword@123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user.getId()))
                .thenReturn("test-access-token");

        when(jwtService.getAccessTokenExpirationSeconds())
                .thenReturn(900L);

        // Mock refresh-session creation
        UUID sessionId = UUID.randomUUID();

        CreatedSession createdSession = new CreatedSession(
                sessionId,
                "test-refresh-token",
                LocalDateTime.now().plusDays(30)
        );

        when(authSessionService.createSession(user.getId()))
                .thenReturn(createdSession);

        // Call method under test
        LoginResponse response = authService.authenticate(request);

        // Assertions
        assertEquals("test-access-token", response.accessToken());
        assertEquals("test-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        // Verifications
        verify(loginStateService)
                .recordSuccess(eq(user.getId()), any(LocalDateTime.class));

        verify(jwtService)
                .generateAccessToken(user.getId());

        verify(authSessionService)
                .createSession(user.getId());

        verify(jwtService)
                .getAccessTokenExpirationSeconds();

        verify(loginStateService, never())
                .recordFailure(any(), any());
    }

    @Test
    void shouldNormalizeEmailBeforeAuthentication() {

        AuthUser user = AuthUser.create(
                "tony@example.com",
                "encoded-password"
        );

        LoginRequest request = new LoginRequest(
                "  TONY@EXAMPLE.COM  ",
                "StrongPassword@123"
        );

        CreatedSession createdSession = new CreatedSession(
                UUID.randomUUID(),
                "test-refresh-token",
                LocalDateTime.now().plusDays(30)
        );

        when(authSessionService.createSession(user.getId()))
                .thenReturn(createdSession);

        when(jwtService.generateAccessToken(user.getId()))
                .thenReturn("test-access-token");

        when(jwtService.getAccessTokenExpirationSeconds())
                .thenReturn(900L);

        when(authUserRepository.findByEmail("tony@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        authService.authenticate(request);

        verify(authUserRepository)
                .findByEmail("tony@example.com");

        verify(jwtService)
                .generateAccessToken(user.getId());

    }
}
