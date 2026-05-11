package com.codeWithProject.ecom.service.dto;

import com.codeWithProject.ecom.entity.enums.StatutCandidature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureResponseDTO {

    private Long id;

    private Long offreId;
    private Long employeId;

    private String employeNom;
    private String employePrenom;
    private String employeEmail;
    private String employePosteActuel;
    private String employeDepartement;

    // IMPORTANT : ce champ est nécessaire pour .employePhotoUrl(...) dans le builder
    private String employePhotoUrl;

    private String motivation;

    private String cvFileName;
    private String cvUrl;

    private StatutCandidature statut;

    private LocalDateTime dateSoumission;
    private LocalDateTime dateDecision;

    private String decisionCommentaire;

    private OffreRecrutementResponse offre;

    private CvAnalyseResponseDTO analyseCv;

    private RecrutementScoreResponse score;
}