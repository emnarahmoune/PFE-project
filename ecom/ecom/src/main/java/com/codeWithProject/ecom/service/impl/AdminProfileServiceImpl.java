package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.AdminProfileService;
import com.codeWithProject.ecom.service.KeycloakAdminService;
import com.codeWithProject.ecom.service.dto.AdminProfileDTO;
import com.codeWithProject.ecom.service.dto.AdminProfileUpdateDTO;
import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminProfileServiceImpl implements AdminProfileService {

    private final AdministrateurRHRepository administrateurRHRepository;
    private final EmployeRepository employeRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;

    @Value("${app.photo.storage.path:./uploads/photos}")
    private String storagePath;

    @Override
    @Transactional(readOnly = true)
    public AdminProfileDTO getProfile(String email) {
        AdministrateurRH admin = findAdminByEmail(email);
        return convertToDto(admin);
    }

    @Override
    public AdminProfileDTO updateProfile(String email, AdminProfileUpdateDTO dto) {
        AdministrateurRH admin = findAdminByEmail(email);

        if (dto.getEmail() != null
                && !dto.getEmail().isBlank()
                && !admin.getEmail().equalsIgnoreCase(dto.getEmail().trim())) {

            administrateurRHRepository.findByEmailIgnoreCase(dto.getEmail().trim())
                    .filter(other -> !other.getId().equals(admin.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Cet email est déjà utilisé");
                    });

            admin.setEmail(dto.getEmail().trim().toLowerCase());
        }

        if (dto.getNom() != null) {
            admin.setNom(dto.getNom());
        }

        if (dto.getPrenom() != null) {
            admin.setPrenom(dto.getPrenom());
        }

        if (dto.getTelephone() != null) {
            admin.setTelephone(dto.getTelephone());
        }

        if (dto.getPoste() != null) {
            admin.setPoste(dto.getPoste());
        }

        if (dto.getDepartement() != null) {
            admin.setDepartement(dto.getDepartement());
        }

        AdministrateurRH saved = administrateurRHRepository.save(admin);
        return convertToDto(saved);
    }

    @Override
    public void changePassword(Jwt jwt, ChangePasswordRequest dto) {
        String email = extractEmail(jwt);

        log.info("CHANGE ADMIN PASSWORD - email={}", email);

        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Employé introuvable : email vide");
        }

        Employe admin = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + email));

        if (!"ADMIN_RH".equalsIgnoreCase(admin.getRole())
                && !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new ResourceNotFoundException("Cet employé n'est pas un administrateur RH : " + email);
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

        if (jwt == null || jwt.getTokenValue() == null || jwt.getTokenValue().isBlank()) {
            throw new BusinessException("Token utilisateur Keycloak introuvable");
        }

        /*
         * Important :
         * On change d'abord le mot de passe dans Keycloak avec le token utilisateur.
         * Si Keycloak échoue, on ne modifie pas la base locale.
         */
        boolean oldPasswordOk = keycloakAdminService.verifyPassword(
        admin.getEmail(),
        dto.getOldPassword()
);

if (!oldPasswordOk) {
    throw new BusinessException("Ancien mot de passe incorrect");
}

if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
    throw new BusinessException("ID utilisateur Keycloak introuvable");
}

keycloakAdminService.resetPassword(
        jwt.getSubject(),
        dto.getNewPassword()
);

admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
employeRepository.saveAndFlush(admin);

        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        employeRepository.saveAndFlush(admin);

        log.info("Mot de passe admin modifié avec succès - id={}, email={}",
                admin.getId(),
                admin.getEmail());
    }

    @Override
    public String uploadPhoto(String email, MultipartFile file) {
        AdministrateurRH admin = findAdminByEmail(email);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fichier photo obligatoire");
        }

        try {
            Path uploadPath = Paths.get(storagePath);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();

            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(newFileName);

            Files.write(filePath, file.getBytes());

            if (admin.getPhotoUrl() != null && !admin.getPhotoUrl().isBlank()) {
                deleteOldPhoto(admin.getPhotoUrl());
            }

            String photoUrl = "/api/photos/" + newFileName;

            admin.setPhotoUrl(photoUrl);
            administrateurRHRepository.save(admin);

            log.info("Photo uploadée pour admin {} : {}", email, photoUrl);

            return photoUrl;

        } catch (IOException e) {
            log.error("Erreur upload photo admin", e);
            throw new RuntimeException("Impossible de sauvegarder la photo", e);
        }
    }

    @Override
    public void deletePhoto(String email) {
        AdministrateurRH admin = findAdminByEmail(email);

        if (admin.getPhotoUrl() != null && !admin.getPhotoUrl().isBlank()) {
            deleteOldPhoto(admin.getPhotoUrl());
            admin.setPhotoUrl(null);
            administrateurRHRepository.save(admin);
        }
    }

    private AdministrateurRH findAdminByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("AdministrateurRH", "email", email);
        }

        return administrateurRHRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("AdministrateurRH", "email", email));
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

    private AdminProfileDTO convertToDto(AdministrateurRH admin) {
        long anciennete = admin.getDateEmbauche() != null
                ? ChronoUnit.YEARS.between(admin.getDateEmbauche(), LocalDate.now())
                : 0L;

        return AdminProfileDTO.builder()
                .id(admin.getId())
                .matricule(admin.getMatricule())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .email(admin.getEmail())
                .telephone(admin.getTelephone())
                .poste(admin.getPoste())
                .dateEmbauche(admin.getDateEmbauche())
                .departement(admin.getDepartement())
                .actif(admin.getActif())
                .role(admin.getRole())
                .statutCompte(admin.getStatut())
                .anciennete(anciennete)
                .photoUrl(admin.getPhotoUrl())
                .build();
    }

    private void deleteOldPhoto(String photoUrl) {
        try {
            String fileName = photoUrl.substring(photoUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(storagePath).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Impossible de supprimer l'ancienne photo : {}", e.getMessage());
        }
    }
}