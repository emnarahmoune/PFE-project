package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO pour l'entité ScoreTurnover
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreTurnoverDTO {

    private Long id;
    private Double score;
    private String niveauRisque; // FAIBLE, MOYEN, ELEVE, CRITIQUE
    private LocalDate datePrediction;
    private String periodePrediction;
    private String facteursPrincipaux;
    private Double confianceModele;
    private String versionModele;
    private String actionRecommandee;

private String employeEmail;
private String employePhotoUrl;
    // Scores individuels
    private Double scoreAnciennete;
    private Double scoreSalaire;
    private Double scorePerformance;
    private Double scoreFormation;
    private Double scoreAbsenteisme;

    // Informations employé
    private Long employeId;
    private String employeMatricule;
    private String employeNom;
    private String employePrenom;
    private String employeDepartement;

    // Informations système BI
    private Long systemeBIId;
    private String systemeBIVersion;

    // Métadonnées
    private Boolean necessiteAlerte;
    private String couleurAffichage;
    private String resume;
    private Boolean valide;
}