package com.codeWithProject.ecom.dto.auth;

import com.codeWithProject.ecom.entity.Utilisateur;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private Utilisateur user;
    private String timestamp;
    private int statusCode;
}