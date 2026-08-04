package com.finflow.auth.controller;

import com.finflow.auth.dto.request.LoginRequest;
import com.finflow.auth.dto.request.RegisterRequest;
import com.finflow.auth.dto.response.LoginResponse;
import com.finflow.auth.dto.response.RegisterResponse;
import com.finflow.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response =
                authService.authenticate(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, UUID>> me(
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                Map.of("userId", userId)
        );
    }
}