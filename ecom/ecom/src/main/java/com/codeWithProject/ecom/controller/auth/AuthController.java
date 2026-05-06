package com.codeWithProject.ecom.controller.auth;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.RoleProvisioningService;
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
    private final RoleProvisioningService roleProvisioningService;

    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncUser(@AuthenticationPrincipal Jwt jwt) {

        log.info("=== SYNC USER ===");

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email introuvable dans le token");
        }

        email = email.trim().toLowerCase();

        String prenom = jwt.getClaimAsString("given_name");
        String nom = jwt.getClaimAsString("family_name");

        if (prenom == null || prenom.isBlank()) {
            prenom = email.split("@")[0];
        }

        if (nom == null || nom.isBlank()) {
            nom = prenom.toUpperCase();
        }

        List<String> roles = extractRoles(jwt);
        String roleSpring = extractSpringRole(roles);

        Employe employe = employeRepository.findByEmailIgnoreCase(email).orElse(null);

        if (employe == null) {

            String normalizedRole = roleProvisioningService.normalizeRole(roleSpring);

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
                    .role(normalizedRole)
                    .build();

            employe = employeRepository.save(employe);

            log.info("✅ Nouvel utilisateur créé : email={}, role={}", email, normalizedRole);

        } else {

            if (employe.getNom() == null || employe.getNom().isBlank()) {
                employe.setNom(nom);
            }

            if (employe.getPrenom() == null || employe.getPrenom().isBlank()) {
                employe.setPrenom(prenom);
            }

            /*
             * 🔥 IMPORTANT :
             * Ne jamais écraser un rôle déjà contrôlé en DB.
             * On affecte le rôle Keycloak seulement si le rôle DB est vide.
             */
            if (employe.getRole() == null || employe.getRole().isBlank()) {
                employe.setRole(roleProvisioningService.normalizeRole(roleSpring));
            }

            if (employe.getPassword() == null || employe.getPassword().isBlank()) {
                employe.setPassword("keycloak-auth");
            }
        }

        employe.setDerniereConnexion(LocalDateTime.now());
        employe.setNombreConnexions(
                Optional.ofNullable(employe.getNombreConnexions()).orElse(0) + 1
        );

        employe = employeRepository.save(employe);

        // ✅ synchronise automatiquement managers / administrateurs RH
        roleProvisioningService.provisionRole(employe);

        Map<String, Object> response = new HashMap<>();
        response.put("id", employe.getId());
        response.put("email", employe.getEmail());
        response.put("nom", employe.getNom());
        response.put("prenom", employe.getPrenom());
        response.put("role", employe.getRole());

        log.info("✅ SYNC OK : email={}, role={}", employe.getEmail(), employe.getRole());

        return ResponseEntity.ok(response);
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Object rolesObject = realmAccess.get("roles");

            if (rolesObject instanceof List<?> list) {
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList();
            }
        }

        return List.of();
    }

    private String extractSpringRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }

        for (String r : roles) {
            if ("ADMIN_RH".equalsIgnoreCase(r) || "ADMIN".equalsIgnoreCase(r) || "RH".equalsIgnoreCase(r)) {
                return "ADMIN_RH";
            }

            if ("MANAGER".equalsIgnoreCase(r)) {
                return "MANAGER";
            }
        }

        return "USER";
    }

    private String generateMatricule() {
        return "EMP" + System.currentTimeMillis();
    }
}