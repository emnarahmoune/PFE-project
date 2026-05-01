package com.codeWithProject.ecom.controller.auth;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final EmployeRepository employeRepository;
    private final EntityManager entityManager;

    // ✅ IMPORTANT : POST (et pas GET)
    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncUser(@AuthenticationPrincipal Jwt jwt) {

        log.info("=== SYNC USER ===");

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        // 🔹 1. EMAIL
        String email = jwt.getClaimAsString("email");

// 🔥 fallback si email absent
if (email == null || email.isEmpty()) {
    email = jwt.getClaimAsString("preferred_username");
}

if (email == null || email.isEmpty()) {
    throw new RuntimeException("Email introuvable dans le token");
}
email = email.trim().toLowerCase();


        // 🔹 2. INFOS KEYCLOAK
        String prenom = jwt.getClaimAsString("given_name");
        String nom = jwt.getClaimAsString("family_name");

        if (prenom == null) prenom = email.split("@")[0];
        if (nom == null) nom = prenom.toUpperCase();

        // 🔹 3. ROLES
        List<String> roles = extractRoles(jwt);
        String roleSpring;

        if (roles.contains("ADMIN_RH") || roles.contains("ADMIN")) {
            roleSpring = "ADMIN_RH";
        } else if (roles.contains("MANAGER")) {
            roleSpring = "manager";
        } else {
            roleSpring = "user";
        }

        // 🔹 4. CHECK EXISTING USER
        Employe employe = employeRepository.findByEmail(email).orElse(null);

        if (employe == null) {
            // ✅ CREATION
            employe = Employe.builder()
                    .matricule(generateMatricule())
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone("")
                    .password("keycloak-auth")
                    .dateEmbauche(LocalDate.now())
                    .poste("À définir")
                    .salaire(0.0)
                    .statut("ACTIF")
                    .departement("À définir")
                    .soldeConges(25)
                    .actif(true)
                    .compteVerrouille(false)
                    .nombreConnexions(0)
                    .tentativesEchec(0)
                    .dateCreation(LocalDate.now())
                    .role(roleSpring)
                    .build();

            employe = employeRepository.save(employe);
            log.info("✅ Nouvel utilisateur créé");

        } else {
            // ✅ UPDATE USER EXISTANT

            // 🔥 CORRECTION IMPORTANTE (username manquant)
            if (employe.getNom() == null || employe.getNom().isEmpty()) {
                employe.setNom(nom);
            }

            if (employe.getPrenom() == null || employe.getPrenom().isEmpty()) {
                employe.setPrenom(prenom);
            }

            // update role
            if (!roleSpring.equals(employe.getRole())) {
                employe.setRole(roleSpring);
            }

            // password fallback
            if (employe.getPassword() == null) {
                employe.setPassword("keycloak-auth");
            }
        }

        // 🔹 5. UPDATE CONNEXION
        employe.setDerniereConnexion(LocalDateTime.now());
        employe.setNombreConnexions(
                Optional.ofNullable(employe.getNombreConnexions()).orElse(0) + 1
        );

        employe = employeRepository.save(employe);

        // 🔹 6. RESPONSE
        Map<String, Object> response = new HashMap<>();
        response.put("id", employe.getId());
        response.put("email", employe.getEmail());
        response.put("nom", employe.getNom());
        response.put("prenom", employe.getPrenom());
        response.put("role", employe.getRole());

        log.info("✅ SYNC OK");

        return ResponseEntity.ok(response);
    }

    // 🔹 EXTRACTION ROLES KEYCLOAK
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    // 🔹 GENERATE MATRICULE
    private String generateMatricule() {
        return "EMP" + System.currentTimeMillis();
    }
}