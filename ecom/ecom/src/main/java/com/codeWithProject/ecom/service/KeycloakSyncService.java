package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakSyncService {

    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Employe syncUser(Jwt jwt) {

        String email = extractEmail(jwt);

        String nom = jwt.getClaimAsString("family_name");
        String prenom = jwt.getClaimAsString("given_name");

        Optional<Employe> optionalEmploye = employeRepository.findByEmail(email);

        Employe employe = optionalEmploye.orElseGet(() -> {
            Employe newEmp = new Employe();
            newEmp.setEmail(email);
            newEmp.setPassword(passwordEncoder.encode("keycloak-auth"));
            newEmp.setActif(true);
            newEmp.setCompteVerrouille(false);
            newEmp.setDateEmbauche(LocalDate.now());
            newEmp.setSalaire(BigDecimal.ZERO);
            newEmp.setStatut("ACTIF");
            newEmp.setSoldeConges(25);
            newEmp.setMatricule(generateMatricule());

            // 🔥 ROLE UNIQUEMENT À LA CRÉATION
            newEmp.setRole("USER");

            return newEmp;
        });

        // 🔥 NE JAMAIS TOUCHER ROLE ICI
        log.error("⛔ ROLE CONSERVÉ = {}", employe.getRole());

        // MAJ INFOS
        employe.setNom(nom != null ? nom.toUpperCase() : "NOM");
        employe.setPrenom(prenom != null ? prenom : "PRENOM");

        employe = employeRepository.save(employe);

        // 🔥 SYNC MANAGER SANS TOUCHER ROLE
        if ("MANAGER".equalsIgnoreCase(employe.getRole())) {

            Manager manager = managerRepository.findById(employe.getId())
                    .orElse(new Manager());

            manager.setId(employe.getId());
            manager.setEmail(employe.getEmail());
            manager.setNom(employe.getNom());
            manager.setPrenom(employe.getPrenom());
            manager.setRole("MANAGER");

            managerRepository.save(manager);
        }

        return employe;
    }

    private String extractEmail(Jwt jwt) {

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            email = jwt.getSubject();
        }

        return email;
    }

    private String generateMatricule() {
        long count = employeRepository.count() + 1;
        return String.format("EMP%03d", count);
    }
}