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
public class HistoriqueCongeDTO {
    private Long id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String type;
    private String statut;
    private Integer joursOuvres;
    private LocalDate dateDemande;
    private LocalDate dateDecision;
}