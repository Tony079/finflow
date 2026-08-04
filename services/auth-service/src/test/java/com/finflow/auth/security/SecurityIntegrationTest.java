package com.finflow.auth.security;

import com.finflow.auth.config.SecurityConfig;
import com.finflow.auth.controller.AuthController;
import com.finflow.auth.exception.GlobalExceptionHandler;
import com.finflow.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/auth/me")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/me"));
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid()
            throws Exception {

        when(jwtService.isTokenValid("invalid-token"))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"));
    }

    @Test
    void shouldAllowAccessWhenTokenIsValid()
            throws Exception {

        UUID userId = UUID.randomUUID();

        when(jwtService.isTokenValid("valid-token"))
                .thenReturn(true);

        when(jwtService.extractUserId("valid-token"))
                .thenReturn(userId);

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .header(
                                        "Authorization",
                                        "Bearer valid-token"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()));
    }
}