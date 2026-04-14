package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.dto.auth.AuthResponse;
import com.codeWithProject.ecom.dto.auth.LoginRequest;
import com.codeWithProject.ecom.dto.auth.RegisterRequest;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.AuthService;
import com.codeWithProject.ecom.service.mapper.EmployeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EmployeRepository employeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeMapper employeMapper;   // ✅ Injecté pour convertir en DTO

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (employeRepository.findByEmail(request.getEmail()).isPresent()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Email déjà utilisé")
                    .statusCode(400)
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        Employe employe = Employe.builder()
                .matricule(generateMatricule())
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .password(passwordEncoder.encode(request.getPassword()))
                .dateEmbauche(LocalDate.now())
                .salaire(0.0)
                .statut("ACTIF")
                .soldeConges(25)
                .actif(true)
                .nombreConnexions(0)
                .tentativesEchec(0)
                .compteVerrouille(false)
                .role("user")
                .typeEmploye(Employe.TYPE_EMPLOYE)
                .build();

        Employe saved = employeRepository.save(employe);
        // ✅ Conversion de l'entité en DTO avant de l'ajouter à la réponse
        var userDto = employeMapper.toDto(saved);

        return AuthResponse.builder()
                .success(true)
                .message("Inscription réussie")
                .statusCode(200)
                .timestamp(LocalDateTime.now().toString())
                .user(userDto)   // ✅ DTO attendu par AuthResponse
                .build();
    }

    private String generateMatricule() {
        return "EMP" + System.currentTimeMillis();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return AuthResponse.builder()
                .success(false)
                .message("Utilisez Keycloak pour l'authentification")
                .statusCode(400)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}