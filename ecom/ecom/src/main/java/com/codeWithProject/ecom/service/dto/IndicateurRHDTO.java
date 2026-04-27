package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO pour l'entité IndicateurRH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicateurRHDTO {

    private Long id;
    private String type; // TURNOVER, ABSENTEISME, PERFORMANCE, SATISFACTION, COMPETENCES
    private Double valeur;
    private LocalDate dateCalcul;
    private String periode; // MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL
    private Integer annee;
    private Integer mois;
    private Integer trimestre;
    private String departement;
    private String commentaire;
    private String tendance; // HAUSSE, BAISSE, STABLE
    private Double valeurPrecedente;
    private Double variationPourcentage;

    // Informations système BI
    private Long systemeBIId;
    private String systemeBIVersion;


    private Long employeId;    // Métadonnées pour l'affichage
    private String niveauAlerte;
    private String description;
    private Boolean dansLaNorme;
}