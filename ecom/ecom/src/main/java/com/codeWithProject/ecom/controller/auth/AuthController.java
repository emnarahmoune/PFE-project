package com.codeWithProject.ecom.controller.auth;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;

    @GetMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser(@AuthenticationPrincipal Jwt jwt) {
        log.info("=== SYNC USER - Synchronisation utilisateur Keycloak ===");

        if (jwt == null) {
            log.error("JWT est null");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        log.info("Email extrait du token: {}", email);

        // 1. Extraire les rôles Keycloak et les normaliser en majuscules pour la comparaison
        List<String> roles = extractRoles(jwt);
        List<String> normalizedRoles = roles.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        log.info("Rôles extraits (normalisés): {}", normalizedRoles);

        // 2. Déterminer le type d'utilisateur (colonne type_utilisateur) – valeurs majuscules
        String typeUtilisateur = Utilisateur.TYPE_EMPLOYE; // "UTILISATEUR"
        if (normalizedRoles.contains("ADMIN_RH") || normalizedRoles.contains("ADMIN")) {
            typeUtilisateur = Utilisateur.TYPE_ADMIN_RH; // "ADMIN_RH"
        } else if (normalizedRoles.contains("MANAGER")) {
            typeUtilisateur = Utilisateur.TYPE_MANAGER; // "MANAGER"
        }

        // 3. Déterminer le rôle Spring Security (colonne role) – casse exacte utilisée dans @PreAuthorize
        String roleSpring;
        if (normalizedRoles.contains("ADMIN_RH") || normalizedRoles.contains("ADMIN")) {
            roleSpring = "ADMIN_RH";   // majuscules pour correspondre à @PreAuthorize("hasRole('ADMIN_RH')")
        } else if (normalizedRoles.contains("MANAGER")) {
            roleSpring = "manager";    // minuscule pour correspondre à hasAnyRole('manager')
        } else {
            roleSpring = "user";       // rôle par défaut pour les employés
        }

        // 4. Récupérer ou créer l'utilisateur
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);
        boolean isNew = false;

        if (utilisateur == null) {
            log.info("Utilisateur non trouvé - création automatique...");
            isNew = true;

            String prenom = jwt.getClaimAsString("given_name");
            if (prenom == null) prenom = email.split("@")[0];
            String nom = jwt.getClaimAsString("family_name");
            if (nom == null) nom = prenom.toUpperCase();

            // Générer un matricule unique
            String matricule = generateUniqueMatricule();

            Employe employe = Employe.builder()
                    .matricule(matricule)
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .telephone("")
                    .dateEmbauche(LocalDate.now())
                    .poste("À définir")
                    .salaire(0.0)
                    .statut("ACTIF")
                    .departement("À définir")
                    .soldeConges(25)
                    .build();
            employe = employeRepository.save(employe);
            log.info("✅ Employé créé avec ID: {}, matricule: {}", employe.getId(), matricule);

            utilisateur = new Utilisateur();
            utilisateur.setNom(employe.getNom());
            utilisateur.setPrenom(employe.getPrenom());
            utilisateur.setEmail(employe.getEmail());
            utilisateur.setTelephone(employe.getTelephone());
            utilisateur.setPassword("");
            utilisateur.setActif(true);
            utilisateur.setCompteVerrouille(false);
            utilisateur.setNombreConnexions(0);
            utilisateur.setTentativesEchec(0);
            utilisateur.setDateCreation(LocalDate.now());
            utilisateur.setEmploye(employe);
            utilisateur.setTypeUtilisateur(typeUtilisateur);
            utilisateur.setRole(roleSpring);      // ✅ rôle Spring Security
            utilisateur = utilisateurRepository.save(utilisateur);
            log.info("✅ Utilisateur créé avec ID: {}, type: {}, role: {}", utilisateur.getId(), typeUtilisateur, roleSpring);
        } else {
            log.info("✅ Utilisateur existant trouvé - ID: {}", utilisateur.getId());

            // Vérifier et réparer l'employé associé s'il a disparu
            if (utilisateur.getEmploye() == null) {
                log.warn("⚠️ L'utilisateur {} a un employe_id null – création d'un nouvel employé", email);
                String prenom = jwt.getClaimAsString("given_name");
                if (prenom == null) prenom = email.split("@")[0];
                String nom = jwt.getClaimAsString("family_name");
                if (nom == null) nom = prenom.toUpperCase();

                String matricule = generateUniqueMatricule();
                Employe newEmploye = Employe.builder()
                        .matricule(matricule)
                        .nom(nom)
                        .prenom(prenom)
                        .email(email)
                        .telephone("")
                        .dateEmbauche(LocalDate.now())
                        .poste("À définir")
                        .salaire(0.0)
                        .statut("ACTIF")
                        .departement("À définir")
                        .soldeConges(25)
                        .build();
                newEmploye = employeRepository.save(newEmploye);
                utilisateur.setEmploye(newEmploye);
                log.info("✅ Nouvel employé créé et associé à l'utilisateur: {}", newEmploye.getId());
            }

            // Mise à jour du type utilisateur et du rôle si nécessaire
            boolean needUpdate = false;
            if (!typeUtilisateur.equals(utilisateur.getTypeUtilisateur())) {
                log.info("Changement de type détecté: {} -> {}", utilisateur.getTypeUtilisateur(), typeUtilisateur);
                utilisateur.setTypeUtilisateur(typeUtilisateur);
                needUpdate = true;
            }
            if (!roleSpring.equals(utilisateur.getRole())) {
                log.info("Changement de rôle détecté: {} -> {}", utilisateur.getRole(), roleSpring);
                utilisateur.setRole(roleSpring);
                needUpdate = true;
            }
            if (needUpdate) {
                utilisateur = utilisateurRepository.save(utilisateur);
            }
        }

        // 5. Si manager, créer l'entrée dans la table managers si absente
        if (Utilisateur.TYPE_MANAGER.equals(typeUtilisateur) && !managerRepository.existsById(utilisateur.getId())) {
            Manager manager = new Manager();
            manager.setId(utilisateur.getId());
            manager.setNom(utilisateur.getNom());
            manager.setPrenom(utilisateur.getPrenom());
            manager.setEmail(utilisateur.getEmail());
            manager.setTelephone(utilisateur.getTelephone());
            manager.setActif(utilisateur.getActif());
            manager.setCompteVerrouille(utilisateur.getCompteVerrouille());
            manager.setDateCreation(utilisateur.getDateCreation());
            manager.setDerniereConnexion(utilisateur.getDerniereConnexion());
            manager.setNombreConnexions(utilisateur.getNombreConnexions());
            manager.setTentativesEchec(utilisateur.getTentativesEchec());
            manager.setDateVerrouillage(utilisateur.getDateVerrouillage());
            manager.setTypeUtilisateur(Utilisateur.TYPE_MANAGER);
            manager.setRole(roleSpring);
            manager.setDepartement("GENERAL");
            manager.setDateNomination(LocalDate.now());
            managerRepository.save(manager);
            log.info("✅ Entrée Manager créée pour l'utilisateur {}", email);
        }

        // 6. Mettre à jour la dernière connexion
        utilisateur.setDerniereConnexion(LocalDateTime.now());
        if (utilisateur.getNombreConnexions() == null) utilisateur.setNombreConnexions(0);
        utilisateur.setNombreConnexions(utilisateur.getNombreConnexions() + 1);
        utilisateurRepository.save(utilisateur);

        // 7. Construire la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("nom", utilisateur.getNom());
        response.put("prenom", utilisateur.getPrenom());
        response.put("role", utilisateur.getTypeUtilisateur()); // pour compatibilité frontend
        response.put("employeId", utilisateur.getEmploye() != null ? utilisateur.getEmploye().getId() : null);

        if (utilisateur.getEmploye() != null) {
            response.put("matricule", utilisateur.getEmploye().getMatricule());
            response.put("departement", utilisateur.getEmploye().getDepartement());
            response.put("poste", utilisateur.getEmploye().getPoste());
            response.put("soldeConges", utilisateur.getEmploye().getSoldeConges());
        }

        log.info("=== SYNC USER TERMINÉ AVEC SUCCÈS ===");
        return ResponseEntity.ok(response);
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    /**
     * Génère un matricule unique pour éviter les doublons.
     */
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