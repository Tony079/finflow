package com.finflow.auth.controller;

import com.finflow.auth.config.SecurityConfig;
import com.finflow.auth.domain.UserStatus;
import com.finflow.auth.dto.response.RegisterResponse;
import com.finflow.auth.exception.EmailAlreadyExistsException;
import com.finflow.auth.exception.GlobalExceptionHandler;
import com.finflow.auth.security.JwtService;
import com.finflow.auth.security.RestAuthenticationEntryPoint;
import com.finflow.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void shouldRegisterUserAndReturn201() throws Exception {

        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 7, 8, 22, 30);

        RegisterResponse response = new RegisterResponse(
                userId,
                "tony@example.com",
                UserStatus.ACTIVE,
                false,
                createdAt
        );

        when(authService.register(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "tony@example.com",
                                  "password": "StrongPassword@123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("tony@example.com"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified")
                        .value(false))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(authService).register(any());
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.email")
                        .value("Email format is invalid"))
                .andExpect(jsonPath("$.validationErrors.password")
                        .value("Password must be between 8 and 72 characters"));

        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {

        when(authService.register(any()))
                .thenThrow(new EmailAlreadyExistsException(
                        "An account already exists with this email"
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "tony@example.com",
                                  "password": "StrongPassword@123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("An account already exists with this email"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"));

        verify(authService).register(any());
    }

    @Test
    void shouldAllowRegistrationWithoutAuthentication() throws Exception {

        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(),
                "tony@example.com",
                UserStatus.ACTIVE,
                false,
                LocalDateTime.now()
        );

        when(authService.register(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "tony@example.com",
                                  "password": "StrongPassword@123"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}