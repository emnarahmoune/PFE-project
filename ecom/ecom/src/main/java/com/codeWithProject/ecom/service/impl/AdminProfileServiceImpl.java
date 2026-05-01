// service/impl/AdminProfileServiceImpl.java
package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.service.AdminProfileService;
import com.codeWithProject.ecom.service.dto.AdminProfileDTO;
import com.codeWithProject.ecom.service.dto.AdminProfileUpdateDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

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
        if (!admin.getEmail().equalsIgnoreCase(dto.getEmail())) {
            administrateurRHRepository.findByEmail(dto.getEmail())
                    .filter(other -> !other.getId().equals(admin.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Cet email est déjà utilisé");
                    });
        }
        admin.setNom(dto.getNom());
        admin.setPrenom(dto.getPrenom());
        admin.setEmail(dto.getEmail().toLowerCase());
        admin.setTelephone(dto.getTelephone());
        admin.setPoste(dto.getPoste());
        admin.setDepartement(dto.getDepartement());

        AdministrateurRH saved = administrateurRHRepository.save(admin);
        return convertToDto(saved);
    }

    @Override
    public String uploadPhoto(String email, MultipartFile file) {
        AdministrateurRH admin = findAdminByEmail(email);
        try {
            Path uploadPath = Paths.get(storagePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String newFileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(newFileName);
            Files.write(filePath, file.getBytes());

            // Supprimer l'ancienne photo
            if (admin.getPhotoUrl() != null) {
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
        if (admin.getPhotoUrl() != null) {
            deleteOldPhoto(admin.getPhotoUrl());
            admin.setPhotoUrl(null);
            administrateurRHRepository.save(admin);
        }
    }

    // ========== PRIVATE ==========

    private AdministrateurRH findAdminByEmail(String email) {
        return administrateurRHRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AdministrateurRH", "email", email));
    }

    private AdminProfileDTO convertToDto(AdministrateurRH admin) {
        long anciennete = (admin.getDateEmbauche() != null)
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
                .statutCompte(admin.getStatutCompte())
                .anciennete(anciennete)
                .photoUrl(admin.getPhotoUrl())
                 // AJOUT
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