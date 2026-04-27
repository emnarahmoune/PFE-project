package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO pour l'entité DemandeConge
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCongeDTO {

    private Long id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String type;
    private Long adminRhId;
    private String adminRhNom;
    private String statut;
    private LocalDate dateDemande;
    private LocalDate dateDecision;
    private String commentaire;
    private String motifRefus;
    private Integer joursOuvres;
    private Boolean urgente;

    // Informations employé
    private Long employeId;
    private String employeMatricule;
    private String employeNom;
    private String employePrenom;

    // Informations manager
    private Long managerId;
    private String managerNom;

    // Champs calculés
    private Long nombreJours;
    private String resume;
    // Ajoutez ces champs pour le workflow
    private String managerEmail;
    private String adminEmail;
    private Boolean managerApprouve;
    private Boolean rhApprouve;
    private String processInstanceId;
}