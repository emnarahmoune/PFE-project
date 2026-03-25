package com.codeWithProject.ecom.controller.auth;

import com.codeWithProject.ecom.dto.auth.LoginRequest;
import com.codeWithProject.ecom.dto.auth.RegisterRequest;
import com.codeWithProject.ecom.dto.auth.AuthResponse;
import com.codeWithProject.ecom.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login");
        AuthResponse response = authService.login(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}