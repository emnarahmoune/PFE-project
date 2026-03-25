package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.dto.auth.LoginRequest;
import com.codeWithProject.ecom.dto.auth.RegisterRequest;
import com.codeWithProject.ecom.dto.auth.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}