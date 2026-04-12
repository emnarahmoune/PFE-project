package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakSyncService {

    private final UtilisateurRepository utilisateurRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Utilisateur syncUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String nom = jwt.getClaimAsString("family_name");
        String prenom = jwt.getClaimAsString("given_name");

        // Extraire les rôles du token
        List<String> roles = extractRoles(jwt);
        boolean isManager = roles.contains("manager");

        // Chercher l'utilisateur existant
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseGet(() -> {
                    Utilisateur newUser = new Utilisateur();
                    newUser.setEmail(email);
                    newUser.setPassword(passwordEncoder.encode("keycloak-auth"));
                    newUser.setActif(true);
                    newUser.setCompteVerrouille(false);
                    return newUser;
                });

        user.setNom(nom != null ? nom.toUpperCase() : "NOM");
        user.setPrenom(prenom != null ? prenom : "PRENOM");
        user.setTypeUtilisateur(isManager ? "MANAGER" : "EMPLOYE");
        user.setRole(isManager ? "manager" : "employe");

        utilisateurRepository.save(user);

        // Si c'est un manager, créer l'entrée dans la table `managers` si absente
        if (isManager && !managerRepository.existsById(user.getId())) {
            Manager manager = new Manager();
            // Copier l'identifiant et tous les champs hérités de Utilisateur
            manager.setId(user.getId());
            manager.setNom(user.getNom());
            manager.setPrenom(user.getPrenom());
            manager.setEmail(user.getEmail());
            manager.setTelephone(user.getTelephone());
            manager.setActif(user.getActif());
            manager.setCompteVerrouille(user.getCompteVerrouille());
            manager.setDateCreation(user.getDateCreation());
            manager.setDerniereConnexion(user.getDerniereConnexion());
            manager.setNombreConnexions(user.getNombreConnexions());
            manager.setTentativesEchec(user.getTentativesEchec());
            manager.setDateVerrouillage(user.getDateVerrouillage());
            manager.setTypeUtilisateur("MANAGER");
            manager.setRole("manager");

            // Champs spécifiques à Manager
            manager.setDepartement("GENERAL");
            manager.setDateNomination(LocalDate.now());

            managerRepository.save(manager);
            log.info("✅ Manager créé pour l'utilisateur {}", email);
        }

        return user;
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }
}