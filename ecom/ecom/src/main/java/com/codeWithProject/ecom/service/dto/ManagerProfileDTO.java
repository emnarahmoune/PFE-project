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
public class ManagerProfileDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String prenom;

    private String photoUrl;

    @NotBlank(message = "L'email est obligatoire")
    @Email
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^$|^[0-9+\\s()\\-]{6,20}$", message = "Format de téléphone invalide")
    private String telephone;

    @Size(max = 100)
    private String poste;

    private String departement;

    private Long id;
    private String matricule;
    private LocalDate dateEmbauche;
    private LocalDate dateNomination;
    private Boolean actif;
    private String role;
    private Integer soldeConges;
    private String statutCompte;
    private Long anciennete;
    private Long ancienneteManager;
    private Integer nombreEmployesGeres;
    private String nomComplet;

    // ✅ Ajouté pour afficher "3 jours", "2 mois", "1 an et 4 mois"
    private String ancienneteManagerLabel;
}