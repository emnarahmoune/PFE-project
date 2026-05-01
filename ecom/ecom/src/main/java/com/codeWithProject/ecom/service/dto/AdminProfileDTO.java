package com.codeWithProject.ecom.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDTO {

    private Long id;

    @NotBlank(message = "Le matricule est obligatoire")
    @Size(max = 50, message = "Le matricule ne doit pas dépasser 50 caractères")
    private String matricule;


    private String photoUrl;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
    private String email;

    @Pattern(regexp = "^[0-9+\\s()\\-]{6,20}$", message = "Format de téléphone invalide")
    private String telephone;

    @Size(max = 100, message = "Le poste ne doit pas dépasser 100 caractères")
    private String poste;

    private LocalDate dateEmbauche;
    private String departement;
    private Boolean actif;
    private String role;

    // Champs non modifiables, juste pour l'affichage
    private String statutCompte;
    private Long anciennete;
}