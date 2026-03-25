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
public class FormationEmployeDTO {
    private Long id;
    private String titre;
    private String domaine;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private Integer noteEvaluation;
    private Integer progression;
}