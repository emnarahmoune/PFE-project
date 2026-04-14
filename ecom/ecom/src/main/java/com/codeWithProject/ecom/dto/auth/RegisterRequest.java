package com.codeWithProject.ecom.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class RegisterRequest {

    // Champs obligatoires pour Employe
    @NotBlank(message = "Le matricule est obligatoire")
    @Size(min = 3, max = 50, message = "Le matricule doit contenir entre 3 et 50 caractères")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String telephone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    // Champs professionnels obligatoires
    @NotNull(message = "La date d'embauche est obligatoire")
    private LocalDate dateEmbauche;

    @NotBlank(message = "Le poste est obligatoire")
    private String poste;

    @NotNull(message = "Le salaire est obligatoire")
    private Double salaire;

    private String departement;

    // Rôle (optionnel, par défaut "user")
    private String role;  // "user", "manager", "ADMIN_RH"

    // Pour assigner un manager (optionnel)
    private String managerEmail;
}