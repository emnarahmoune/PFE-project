package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
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

    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Employe syncUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String nom = jwt.getClaimAsString("family_name");
        String prenom = jwt.getClaimAsString("given_name");

        // Extraire les rôles du token
        List<String> roles = extractRoles(jwt);
        boolean isManager = roles.stream().anyMatch(r -> r.equalsIgnoreCase("manager"));

        // Chercher l'employé existant
        Employe employe = employeRepository.findByEmail(email).orElseGet(() -> {
            Employe newEmploye = new Employe();
            newEmploye.setEmail(email);
            newEmploye.setPassword(passwordEncoder.encode("keycloak-auth"));
            newEmploye.setActif(true);
            newEmploye.setCompteVerrouille(false);
            newEmploye.setDateEmbauche(LocalDate.now());
            newEmploye.setSalaire(0.0);
            newEmploye.setStatut("ACTIF");
            newEmploye.setSoldeConges(25);
            newEmploye.setMatricule(generateMatricule());
            return newEmploye;
        });

        employe.setNom(nom != null ? nom.toUpperCase() : "NOM");
        employe.setPrenom(prenom != null ? prenom : "PRENOM");
        employe.setTypeEmploye(isManager ? Employe.TYPE_MANAGER : Employe.TYPE_EMPLOYE);
        employe.setRole(isManager ? "manager" : "user");

        employe = employeRepository.save(employe);

        // Si c'est un manager, créer l'entrée dans la table `managers` si absente
        if (isManager && !managerRepository.existsById(employe.getId())) {
            Manager manager = new Manager();
            manager.setId(employe.getId());
            manager.setMatricule(employe.getMatricule());
            manager.setNom(employe.getNom());
            manager.setPrenom(employe.getPrenom());
            manager.setEmail(employe.getEmail());
            manager.setTelephone(employe.getTelephone());
            manager.setDateEmbauche(employe.getDateEmbauche());
            manager.setPoste(employe.getPoste());
            manager.setSalaire(employe.getSalaire());
            manager.setStatut(employe.getStatut());
            manager.setDepartement("GENERAL");
            manager.setSoldeConges(employe.getSoldeConges());
            manager.setActif(employe.getActif());
            manager.setCompteVerrouille(employe.getCompteVerrouille());
            manager.setDateCreation(employe.getDateCreation());
            manager.setDerniereConnexion(employe.getDerniereConnexion());
            manager.setNombreConnexions(employe.getNombreConnexions());
            manager.setTentativesEchec(employe.getTentativesEchec());
            manager.setDateVerrouillage(employe.getDateVerrouillage());
            manager.setRole("manager");
            manager.setTypeEmploye(Employe.TYPE_MANAGER);
            manager.setDateNomination(LocalDate.now());

            managerRepository.save(manager);
            log.info("✅ Manager créé pour l'employé {}", email);
        }

        return employe;
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    private String generateMatricule() {
        long count = employeRepository.count() + 1;
        return String.format("EMP%03d", count);
    }
}