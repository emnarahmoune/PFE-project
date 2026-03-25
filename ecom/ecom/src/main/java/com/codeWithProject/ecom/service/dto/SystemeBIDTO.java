package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour l'entité SystemeBI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemeBIDTO {

    private Long id;
    private String version;
    private String outilETL;
    private String outilVisualisation;
    private String modeleML;
    private LocalDateTime derniereExecution;
    private String statut; // ACTIF, MAINTENANCE, INACTIF

    // Statistiques
    private Integer nombreIndicateurs;
    private Integer nombreScoresTurnover;
    private List<Long> indicateurIds;
    private List<Long> scoreIds;

    // Métadonnées
    private Boolean operationnel;
    private String dernierResumeExecution;

    // Pour les recommandations
    private List<FormationDTO> formationsRecommandees;
}