package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.PhotoService;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoServiceImpl implements PhotoService {

    private final EmployeRepository employeRepository;

    @Value("${app.photo.storage.path:./uploads/photos}")
    private String storagePath;

    @Override
    @Transactional
    public String uploadPhoto(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new RuntimeException("ID utilisateur obligatoire pour upload photo");
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fichier photo vide ou manquant");
        }

        Employe employe = employeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", userId));

        try {
            Path uploadPath = Paths.get(storagePath);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();

            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            if (!extension.equals(".jpg")
                    && !extension.equals(".jpeg")
                    && !extension.equals(".png")
                    && !extension.equals(".gif")
                    && !extension.equals(".webp")) {
                throw new RuntimeException("Format image non supporté");
            }

            String newFileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(newFileName);

            Files.write(filePath, file.getBytes());

            if (employe.getPhotoUrl() != null && !employe.getPhotoUrl().isBlank()) {
                deleteOldPhoto(employe.getPhotoUrl());
            }

            String relativeUrl = "/api/photos/" + newFileName;

            employe.setPhotoUrl(relativeUrl);
            employeRepository.save(employe);

            log.info("Photo uploadée pour userId={} : {}", userId, relativeUrl);

            return relativeUrl;

        } catch (IOException e) {
            log.error("Erreur lors de l'upload de la photo", e);
            throw new RuntimeException("Impossible de sauvegarder la photo", e);
        }
    }

    @Override
    public byte[] getPhoto(String photoUrl) {
        try {
            if (photoUrl == null || photoUrl.isBlank()) {
                return null;
            }

            String fileName = photoUrl.substring(photoUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(storagePath).resolve(fileName);

            if (!Files.exists(filePath)) {
                return null;
            }

            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            log.error("Erreur lecture photo", e);
            return null;
        }
    }

    @Override
    @Transactional
    public void deletePhoto(Long userId) {
        if (userId == null) {
            throw new RuntimeException("ID utilisateur obligatoire pour suppression photo");
        }

        Employe employe = employeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", userId));

        if (employe.getPhotoUrl() != null && !employe.getPhotoUrl().isBlank()) {
            deleteOldPhoto(employe.getPhotoUrl());
            employe.setPhotoUrl(null);
            employeRepository.save(employe);
        }
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