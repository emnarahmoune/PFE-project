package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDTO {

    private Long id;

    private Long employeId;
    private String employeNom;
    private String employePrenom;
    private String employeEmail;
    private String employePoste;
    private String employeDepartement;

    private LocalDate dateEvaluation;
    private String periode;

    /**
     * Note globale sur 10 côté backend.
     */
    private Double note;

    /**
     * Alias pour le front.
     */
    private Double noteGlobale;

    /**
     * Notes détaillées sur 10 côté backend.
     */
    private Double noteTechnique;
    private Double noteCommunication;
    private Double noteLeadership;
    private Double notePonctualite;
    private Double noteProductivite;

    private Integer objectifsAtteints;

    private String commentaire;

    private String pointsForts;
    private String axesAmelioration;
    private String commentaireManager;
    private String objectifs;

    private Long evaluateurId;
    private String evaluateurNom;
    private String evaluateurPrenom;
    private String evaluateurEmail;

    /**
     * Alias manager pour le front employé/admin.
     */
    private Long managerId;
    private String managerNom;
    private String managerPrenom;
    private String managerEmail;


    private String evaluateurRole;

    private String niveauPerformance;

    private String statut;
}