package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour l'entité Manager
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDTO {

    private Long id;
    private String departement;
    private LocalDate dateNomination;
    private Boolean actif;

    // Informations utilisateur
    private Long utilisateurId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    // Informations employé associé
    private Long employeId;
    private String employeMatricule;
    private String employeNom;
    private String employePrenom;

    // Statistiques
    private Integer nombreEmployesGeres;
    private Integer nombreEmployesTotal;
    private Long nombreDemandesEnAttente;
    private List<Long> employesGeresIds;
    private List<Long> demandesEnAttenteIds;

    // Métadonnées
    private String nomComplet;
    private String matricule;
    private Long ancienneteManager;

    // Pour le tableau de bord
    private Boolean aDesDemandesUrgentes;
    private Integer nombreDemandesUrgentes;
}