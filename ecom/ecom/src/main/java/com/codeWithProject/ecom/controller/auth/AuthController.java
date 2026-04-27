package com.codeWithProject.ecom.controller.auth;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final EntityManager entityManager;

    @GetMapping("/sync")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncUser(@AuthenticationPrincipal Jwt jwt) {
        log.info("=== SYNC USER - Synchronisation utilisateur Keycloak ===");

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("❌ Email introuvable dans le token");
        }
        email = email.trim().toLowerCase();
        log.info("Email extrait du token: {}", email);

        // 1. Extraire les rôles Keycloak
        List<String> roles = extractRoles(jwt);
        List<String> normalizedRoles = roles.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        log.info("Rôles extraits (normalisés): {}", normalizedRoles);

        // 2. Déterminer le type d'employé et le rôle Spring
        String targetType;   // "EMPLOYE", "MANAGER", "ADMIN_RH"
        String roleSpring;

        if (normalizedRoles.contains("ADMIN_RH") || normalizedRoles.contains("ADMIN")) {
            targetType = Employe.TYPE_ADMIN_RH;
            roleSpring = "ADMIN_RH";
        } else if (normalizedRoles.contains("MANAGER")) {
            targetType = Employe.TYPE_MANAGER;
            roleSpring = "manager";
        } else {
            targetType = Employe.TYPE_EMPLOYE;
            roleSpring = "user";
        }

        // 3. Récupérer l'employé existant
        Employe employe = employeRepository.findByEmail(email).orElse(null);

        if (employe == null) {
            // Création d'un nouvel employé avec la bonne sous-classe
            employe = createEmployeOfType(jwt, email, targetType, roleSpring);
            log.info("✅ Nouvel employé créé - ID: {}, type: {}", employe.getId(), targetType);
        } else {
            // Mise à jour : vérifier si le type a changé
            String currentType = employe.getTypeEmploye();
            if (!targetType.equals(currentType)) {
                log.info("Changement de type détecté: {} -> {}", currentType, targetType);
                convertEmployeType(employe, targetType);
                // Recharger l'employé après conversion
                employe = employeRepository.findById(employe.getId()).orElse(employe);
            }

            // Mise à jour du rôle Spring si nécessaire
            if (!roleSpring.equals(employe.getRole())) {
                employe.setRole(roleSpring);
                employe = employeRepository.save(employe);
            }

            // Mise à jour du mot de passe si absent
            if (employe.getPassword() == null || employe.getPassword().isEmpty()) {
                employe.setPassword("keycloak-auth");
                employe = employeRepository.save(employe);
            }
        }

        // 4. Mettre à jour la dernière connexion
        employe.setDerniereConnexion(LocalDateTime.now());
        if (employe.getNombreConnexions() == null) employe.setNombreConnexions(0);
        employe.setNombreConnexions(employe.getNombreConnexions() + 1);
        employe = employeRepository.save(employe);

        // 5. Construire la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("id", employe.getId());
        response.put("email", employe.getEmail());
        response.put("nom", employe.getNom());
        response.put("prenom", employe.getPrenom());
        response.put("role", employe.getTypeEmploye());
        response.put("employeId", employe.getId());
        response.put("matricule", employe.getMatricule());
        response.put("departement", employe.getDepartement());
        response.put("poste", employe.getPoste());
        response.put("soldeConges", employe.getSoldeConges());

        log.info("=== SYNC USER TERMINÉ AVEC SUCCÈS ===");
        return ResponseEntity.ok(response);
    }

    /**
     * Crée un employé de la classe concrète appropriée.
     */
    private Employe createEmployeOfType(Jwt jwt, String email, String targetType, String roleSpring) {
        String prenom = jwt.getClaimAsString("given_name");
        if (prenom == null) prenom = email.split("@")[0];
        String nom = jwt.getClaimAsString("family_name");
        if (nom == null) nom = prenom.toUpperCase();
        String matricule = generateUniqueMatricule();

        Employe employe;
        if (Employe.TYPE_MANAGER.equals(targetType)) {
            Manager manager = new Manager();
            manager.setMatricule(matricule);
            manager.setNom(nom);
            manager.setPrenom(prenom);
            manager.setEmail(email);
            manager.setTelephone("");
            manager.setPassword("keycloak-auth");
            manager.setDateEmbauche(LocalDate.now());
            manager.setPoste("À définir");
            manager.setSalaire(0.0);
            manager.setStatut("ACTIF");
            manager.setDepartement("À définir");
            manager.setSoldeConges(25);
            manager.setActif(true);
            manager.setCompteVerrouille(false);
            manager.setNombreConnexions(0);
            manager.setTentativesEchec(0);
            manager.setDateCreation(LocalDate.now());
            manager.setRole(roleSpring);
            manager.setDateNomination(LocalDate.now());
            employe = manager;
        } else if (Employe.TYPE_ADMIN_RH.equals(targetType)) {
            AdministrateurRH admin = new AdministrateurRH();
            admin.setMatricule(matricule);
            admin.setNom(nom);
            admin.setPrenom(prenom);
            admin.setEmail(email);
            admin.setTelephone("");
            admin.setPassword("keycloak-auth");
            admin.setDateEmbauche(LocalDate.now());
            admin.setPoste("À définir");
            admin.setSalaire(0.0);
            admin.setStatut("ACTIF");
            admin.setDepartement("À définir");
            admin.setSoldeConges(25);
            admin.setActif(true);
            admin.setCompteVerrouille(false);
            admin.setNombreConnexions(0);
            admin.setTentativesEchec(0);
            admin.setDateCreation(LocalDate.now());
            admin.setRole(roleSpring);
            employe = admin;
        } else {
            employe = Employe.builder()
                    .matricule(matricule)
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
        }
        return employeRepository.save(employe);
    }

    /**
     * Convertit un employé existant vers un nouveau type (EMPLOYE, MANAGER, ADMIN_RH).
     * Utilise des requêtes natives pour mettre à jour le discriminateur et les tables filles.
     */
    private void convertEmployeType(Employe employe, String newType) {
        Long id = employe.getId();
        if (Employe.TYPE_MANAGER.equals(newType)) {
            // Mettre à jour le discriminateur
            entityManager.createNativeQuery("UPDATE employes SET type_employe = 'MANAGER' WHERE id = ?")
                    .setParameter(1, id)
                    .executeUpdate();
            // Insérer dans la table managers
            entityManager.createNativeQuery("INSERT INTO managers (employe_id, date_nomination) VALUES (?, CURDATE())")
                    .setParameter(1, id)
                    .executeUpdate();
        } else if (Employe.TYPE_ADMIN_RH.equals(newType)) {
            entityManager.createNativeQuery("UPDATE employes SET type_employe = 'ADMIN_RH' WHERE id = ?")
                    .setParameter(1, id)
                    .executeUpdate();
            entityManager.createNativeQuery("INSERT INTO administrateurs_rh (employe_id) VALUES (?)")
                    .setParameter(1, id)
                    .executeUpdate();
        } else {
            // Revenir à EMPLOYE : supprimer les lignes filles
            entityManager.createNativeQuery("DELETE FROM managers WHERE employe_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM administrateurs_rh WHERE employe_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();
            entityManager.createNativeQuery("UPDATE employes SET type_employe = 'EMPLOYE' WHERE id = ?")
                    .setParameter(1, id)
                    .executeUpdate();
        }
        // Vider le cache persistence pour refléter les changements
        entityManager.clear();
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    private String generateUniqueMatricule() {
        long count = employeRepository.count();
        int maxAttempts = 100;
        for (int i = 0; i < maxAttempts; i++) {
            count++;
            String matricule = String.format("EMP%03d", count);
            if (!employeRepository.existsByMatricule(matricule)) {
                return matricule;
            }
        }
        return "EMP" + System.currentTimeMillis();
    }
}