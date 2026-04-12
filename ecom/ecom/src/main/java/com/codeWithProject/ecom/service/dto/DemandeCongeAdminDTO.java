package com.codeWithProject.ecom.service.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCongeAdminDTO {
    private Long id;
    private String employeNom;
    private String employePrenom;
    private String employeEmail;
    private Long employeId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer joursOuvres;
    private String type;
    private String commentaire;
    private String statut;
    private LocalDate dateDemande;
    private String processInstanceId;
    private String taskId;
    private String motifRefus;
    private String currentTaskId;
}