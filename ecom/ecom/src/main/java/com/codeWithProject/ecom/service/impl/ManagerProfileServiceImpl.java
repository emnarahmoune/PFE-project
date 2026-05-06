package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.ManagerProfileService;
import com.codeWithProject.ecom.service.dto.ManagerProfileDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import com.codeWithProject.ecom.service.KeycloakAdminService;
import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ManagerProfileServiceImpl implements ManagerProfileService {


private final EmployeRepository employeRepository;
private final PasswordEncoder passwordEncoder;
private final KeycloakAdminService keycloakAdminService;
    @Override
    @Transactional(readOnly = true)
    public ManagerProfileDTO getProfile(Jwt jwt) {
        String email = extractEmail(jwt);

        log.info("GET MANAGER PROFILE - email={}", email);

        Employe manager = findManagerEmployeByEmail(email);

        return convertToDto(manager);
    }



   @Transactional
public void changePassword(Jwt jwt, ChangePasswordRequest dto) {
    if (jwt == null) {
        throw new BusinessException("Utilisateur non authentifié");
    }

    if (dto == null) {
        throw new BusinessException("Données de mot de passe invalides");
    }

    if (dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
        throw new BusinessException("L'ancien mot de passe est obligatoire");
    }

    if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
        throw new BusinessException("Le nouveau mot de passe est obligatoire");
    }

    if (dto.getNewPassword().length() < 6) {
        throw new BusinessException("Le mot de passe doit contenir au moins 6 caractères");
    }

    if (dto.getOldPassword().equals(dto.getNewPassword())) {
        throw new BusinessException("Le nouveau mot de passe doit être différent de l'ancien");
    }

    String email = jwt.getClaimAsString("email");

    if (email == null || email.isBlank()) {
        email = jwt.getClaimAsString("preferred_username");
    }

    if (email == null || email.isBlank()) {
        throw new BusinessException("Email Keycloak introuvable");
    }

    log.info("CHANGE MANAGER PASSWORD - email={}, keycloakUserId={}", email, jwt.getSubject());

    boolean oldPasswordOk = keycloakAdminService.verifyPassword(
            email,
            dto.getOldPassword()
    );

    if (!oldPasswordOk) {
        throw new BusinessException("Ancien mot de passe incorrect");
    }

    String keycloakUserId = jwt.getSubject();

    if (keycloakUserId == null || keycloakUserId.isBlank()) {
        throw new BusinessException("Identifiant Keycloak introuvable");
    }

    keycloakAdminService.resetPassword(
            keycloakUserId,
            dto.getNewPassword()
    );

    /*
     * Ne sauvegarde pas le nouveau mot de passe dans la table employes.
     * Keycloak est la source du mot de passe.
     */
}
    @Override
    public ManagerProfileDTO updateProfile(Jwt jwt, ManagerProfileDTO dto) {
        String email = extractEmail(jwt);

        log.info("UPDATE MANAGER PROFILE - email={}", email);

        Employe manager = findManagerEmployeByEmail(email);

        if (dto.getEmail() != null
                && !dto.getEmail().isBlank()
                && !manager.getEmail().equalsIgnoreCase(dto.getEmail().trim())) {

            employeRepository.findByEmailIgnoreCase(dto.getEmail().trim())
                    .filter(other -> !other.getId().equals(manager.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Cet email est déjà utilisé");
                    });

            manager.setEmail(dto.getEmail().trim().toLowerCase());
        }

        if (dto.getNom() != null) {
            manager.setNom(dto.getNom());
        }

        if (dto.getPrenom() != null) {
            manager.setPrenom(dto.getPrenom());
        }

        if (dto.getTelephone() != null) {
            manager.setTelephone(dto.getTelephone());
        }

        if (dto.getPoste() != null) {
            manager.setPoste(dto.getPoste());
        }

        if (dto.getDepartement() != null) {
            manager.setDepartement(dto.getDepartement());
        }

        Employe saved = employeRepository.save(manager);

        return convertToDto(saved);
    }

    private Employe findManagerEmployeByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Employé introuvable : email vide");
        }

        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + email));

        if (!"MANAGER".equalsIgnoreCase(employe.getRole())) {
            throw new ResourceNotFoundException("Cet employé n'est pas un manager : " + email);
        }

        return employe;
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            email = jwt.getSubject();
        }

        return email != null ? email.trim().toLowerCase() : null;
    }

    private ManagerProfileDTO convertToDto(Employe manager) {
        long anciennete = manager.getDateEmbauche() != null
                ? ChronoUnit.YEARS.between(manager.getDateEmbauche(), LocalDate.now())
                : 0L;

        long ancienneteManager = manager.getDateEmbauche() != null
                ? ChronoUnit.MONTHS.between(manager.getDateEmbauche(), LocalDate.now())
                : 0L;

        String ancienneteManagerLabel = buildAncienneteLabel(manager.getDateEmbauche());

        log.info(
                "PROFILE ANCIENNETE => email={}, dateEmbauche={}, anciennete={}, ancienneteManager={}, label={}",
                manager.getEmail(),
                manager.getDateEmbauche(),
                anciennete,
                ancienneteManager,
                ancienneteManagerLabel
        );

        return ManagerProfileDTO.builder()
                .id(manager.getId())
                .matricule(manager.getMatricule())
                .nom(manager.getNom())
                .prenom(manager.getPrenom())
                .email(manager.getEmail())
                .telephone(manager.getTelephone())
                .poste(manager.getPoste())
                .departement(manager.getDepartement())
                .photoUrl(manager.getPhotoUrl())
                .dateEmbauche(manager.getDateEmbauche())
                .dateNomination(manager.getDateEmbauche())
                .actif(manager.getActif())
                .role(manager.getRole())
                .soldeConges(manager.getSoldeConges())
                .statutCompte(manager.getStatut())
                .anciennete(anciennete)
                .ancienneteManager(ancienneteManager)
                .ancienneteManagerLabel(ancienneteManagerLabel)
                .nombreEmployesGeres(0)
                .nomComplet(buildNomComplet(manager))
                .build();
    }

    private String buildAncienneteLabel(LocalDate dateDebut) {
        if (dateDebut == null) {
            return "Non renseignée";
        }

        LocalDate now = LocalDate.now();

        long years = ChronoUnit.YEARS.between(dateDebut, now);
        LocalDate afterYears = dateDebut.plusYears(years);

        long months = ChronoUnit.MONTHS.between(afterYears, now);
        LocalDate afterMonths = afterYears.plusMonths(months);

        long days = ChronoUnit.DAYS.between(afterMonths, now);

        if (years > 0) {
            return years + " an" + (years > 1 ? "s" : "")
                    + (months > 0 ? " et " + months + " mois" : "");
        }

        if (months > 0) {
            return months + " mois";
        }

        if (days > 0) {
            return days + " jour" + (days > 1 ? "s" : "");
        }

        return "Aujourd’hui";
    }

    private String buildNomComplet(Employe employe) {
        String prenom = employe.getPrenom() != null ? employe.getPrenom() : "";
        String nom = employe.getNom() != null ? employe.getNom() : "";

        return (prenom + " " + nom).trim();
    }
}