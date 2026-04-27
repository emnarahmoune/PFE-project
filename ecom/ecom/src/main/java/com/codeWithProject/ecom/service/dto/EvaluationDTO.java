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
    private LocalDate dateEvaluation;
    private Double note;
    private Integer objectifsAtteints;
    private String commentaire;
    private Long evaluateurId;
    private String evaluateurNom;
}