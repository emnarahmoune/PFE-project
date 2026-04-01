package com.codeWithProject.ecom.security;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final EmployeRepository employeRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("=== CHARGEMENT UTILISATEUR DEPUIS KEYCLOAK ===");
        log.info("Email reçu: {}", email);

        // 1. Chercher l'utilisateur dans la base de données
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);

        // 2. Si l'utilisateur n'existe pas, on le crée automatiquement
        if (utilisateur == null) {
            log.info("UTILISATEUR NON TROUVÉ - CRÉATION AUTOMATIQUE...");

            // Générer un matricule unique
            long count = employeRepository.count() + 1;
            String matricule = String.format("EMP%03d", count);
            log.info("Matricule généré: {}", matricule);

            // Extraire le nom et prénom de l'email
            String prenom = email.split("@")[0];
            String nom = prenom.toUpperCase();

            // Créer l'employé
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
            log.info("Employé créé avec succès - ID: {}", employe.getId());

            // Créer l'utilisateur associé
            utilisateur = new Utilisateur();
            utilisateur.setNom(employe.getNom());
            utilisateur.setPrenom(employe.getPrenom());
            utilisateur.setEmail(employe.getEmail());
            utilisateur.setTelephone(employe.getTelephone());
            utilisateur.setPassword(""); // Géré par Keycloak
            utilisateur.setActif(true);
            utilisateur.setCompteVerrouille(false);
            utilisateur.setNombreConnexions(1);
            utilisateur.setTentativesEchec(0);
            utilisateur.setDateCreation(LocalDate.now());
            utilisateur.setEmploye(employe);

            // Déterminer le type d'utilisateur (rôle)
            // Pour l'admin, l'email doit contenir "admin" (vous pouvez adapter)
            if (email.contains("admin") || email.equals("admin@portail-rh.com")) {
                utilisateur.setTypeUtilisateur("ADMIN_RH");
            } else {
                utilisateur.setTypeUtilisateur("EMPLOYE");
            }

            utilisateur = utilisateurRepository.save(utilisateur);
            log.info("UTILISATEUR CRÉÉ AVEC SUCCÈS !");
            log.info("ID: {}, Email: {}, Rôle: {}", utilisateur.getId(), utilisateur.getEmail(), utilisateur.getTypeUtilisateur());

        } else {
            log.info("UTILISATEUR TROUVÉ EN BASE");
            log.info("ID: {}, Email: {}, Rôle: {}", utilisateur.getId(), utilisateur.getEmail(), utilisateur.getTypeUtilisateur());

            // Mettre à jour la dernière connexion et le nombre de connexions
            utilisateur.setDerniereConnexion(LocalDateTime.now());
            if (utilisateur.getNombreConnexions() == null) {
                utilisateur.setNombreConnexions(0);
            }
            utilisateur.setNombreConnexions(utilisateur.getNombreConnexions() + 1);
            utilisateurRepository.save(utilisateur);
            log.info("Nombre de connexions mis à jour: {}", utilisateur.getNombreConnexions());
        }

        // 3. Déterminer le rôle pour Spring Security
        String role = utilisateur.getTypeUtilisateur() != null ? utilisateur.getTypeUtilisateur() : "EMPLOYE";
        log.info("Rôle attribué pour Spring Security: ROLE_{}", role);

        // 4. Retourner les informations pour Spring Security
        return User.builder()
                .username(utilisateur.getEmail())
                .password("") // Mot de passe vide car géré par Keycloak
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))
                .accountExpired(false)
                .accountLocked(utilisateur.getCompteVerrouille())
                .credentialsExpired(false)
                .disabled(!utilisateur.getActif())
                .build();
    }
}