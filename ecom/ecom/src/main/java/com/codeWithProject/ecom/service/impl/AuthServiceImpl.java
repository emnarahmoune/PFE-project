package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.dto.auth.AuthResponse;
import com.codeWithProject.ecom.dto.auth.LoginRequest;
import com.codeWithProject.ecom.dto.auth.RegisterRequest;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.UtilisateurRepository;
import com.codeWithProject.ecom.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    // Plus de JwtService, plus de AuthenticationManager

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email deja utilise")
                    .statusCode(400)
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        Utilisateur user = Manager.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .password(passwordEncoder.encode(request.getPassword()))
                .actif(true)
                .departement("GENERAL")
                .nombreConnexions(0)
                .tentativesEchec(0)
                .compteVerrouille(false)
                .build();

        utilisateurRepository.save(user);

        return AuthResponse.builder()
                .success(true)
                .message("Inscription reussie")
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Avec Keycloak, la logique de login est gérée par Keycloak
        // Ce endpoint n'est plus utilisé
        return AuthResponse.builder()
                .success(false)
                .message("Utilisez Keycloak pour l'authentification")
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}