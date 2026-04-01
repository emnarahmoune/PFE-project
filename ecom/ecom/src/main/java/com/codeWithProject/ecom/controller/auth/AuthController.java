package com.codeWithProject.ecom.controller.auth;


import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.EmployeRepository;
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
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;

    @GetMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser(@AuthenticationPrincipal Jwt jwt) {
        log.info("=== SYNC USER - Synchronisation utilisateur Keycloak ===");

        if (jwt == null) {
            log.error("JWT est null");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        if (email == null) {
            email = jwt.getSubject();
        }

        log.info("Email extrait du token: {}", email);

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);

        if (utilisateur == null) {
            log.info("Utilisateur non trouvé dans la base ecom - Création automatique...");

            long count = employeRepository.count() + 1;
            String matricule = String.format("EMP%03d", count);
            String prenom = email.split("@")[0];
            String nom = prenom.toUpperCase();

            Employe employe = new Employe();
            employe.setMatricule(matricule);
            employe.setNom(nom);
            employe.setPrenom(prenom);
            employe.setEmail(email);
            employe.setTelephone("");
            employe.setDateEmbauche(LocalDate.now());
            employe.setPoste("À définir");
            employe.setSalaire(0.0);
            employe.setStatut("ACTIF");
            employe.setDepartement("À définir");
            employe.setSoldeConges(25);

            employe = employeRepository.save(employe);
            log.info("✅ Employé créé avec ID: {}", employe.getId());

            utilisateur = new Utilisateur();
            utilisateur.setNom(employe.getNom());
            utilisateur.setPrenom(employe.getPrenom());
            utilisateur.setEmail(employe.getEmail());
            utilisateur.setTelephone(employe.getTelephone());
            utilisateur.setPassword("");
            utilisateur.setActif(true);
            utilisateur.setCompteVerrouille(false);
            utilisateur.setNombreConnexions(1);
            utilisateur.setTentativesEchec(0);
            utilisateur.setDateCreation(LocalDate.now());
            utilisateur.setEmploye(employe);

            if (email.contains("admin")) {
                utilisateur.setTypeUtilisateur("ADMIN_RH");
            } else {
                utilisateur.setTypeUtilisateur("EMPLOYE");
            }

            utilisateur = utilisateurRepository.save(utilisateur);
            log.info("✅ Utilisateur créé avec ID: {}, Rôle: {}", utilisateur.getId(), utilisateur.getTypeUtilisateur());

        } else {
            log.info("✅ Utilisateur trouvé dans la base ecom - ID: {}", utilisateur.getId());

            utilisateur.setDerniereConnexion(LocalDateTime.now());
            if (utilisateur.getNombreConnexions() == null) {
                utilisateur.setNombreConnexions(0);
            }
            utilisateur.setNombreConnexions(utilisateur.getNombreConnexions() + 1);
            utilisateurRepository.save(utilisateur);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("nom", utilisateur.getNom());
        response.put("prenom", utilisateur.getPrenom());
        response.put("role", utilisateur.getTypeUtilisateur());
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
}