package com.codeWithProject.ecom.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilRequest {

    // ── Informations personnelles ─────────────────
    private String telephone;
    private String adresse;
    private String photo;

    private String prenom;
    private String nom;

    // ── Informations professionnelles ─────────────
    private String poste;
    private String departement;

    // ── Changement de mot de passe ────────────────
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;

    // ── Changement d'email ────────────────────────
    private String newEmail;

    // ── Métadonnées ───────────────────────────────
    private Boolean notifyChanges;  // Envoyer une notification email
}