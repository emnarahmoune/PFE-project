package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.AdminProfileDTO;
import com.codeWithProject.ecom.service.dto.AdminProfileUpdateDTO;
import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AdminProfileService {

    /**
     * Récupère le profil de l'administrateur RH authentifié
     */
    AdminProfileDTO getProfile(String email);

    /**
     * Met à jour les informations personnelles de l'admin (hors mot de passe)
     */
    AdminProfileDTO updateProfile(String email, AdminProfileUpdateDTO dto);

    /**
     * Change le mot de passe de l'admin
     */
    /**
     * Upload de la photo de profil de l'admin
     * @param email email de l'admin
     * @param file fichier image
     * @return URL de la photo
     */
    String uploadPhoto(String email, MultipartFile file);

    /**
     * Supprime la photo de profil de l'admin
     * @param email email de l'admin
     */
    void deletePhoto(String email);


      /**
     * Change le mot de passe de l'admin
     */






      void changePassword(Jwt jwt, ChangePasswordRequest dto);
}