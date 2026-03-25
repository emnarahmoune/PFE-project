package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'entité Competence
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetenceDTO {

    private Long id;
    private String nom;
    private String description;
    private String categorie;
    private Long nombreEmployes; // Nombre d'employés avec cette compétence
}