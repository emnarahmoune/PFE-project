package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour l'entité Formation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationDTO {

    private Long id;
    private String titre;
    private String description;
    private String domaine;
    private Integer dureeHeures;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private String urlVideo;
private String pdfPath;

private int nombreParticipants;
private int totalEmployes;
    // Statistiques
    private List<Long> participantIds;


    private Long employeId;
private String employeNom;
private String employePrenom;
private String employeEmail;
private String employePhotoUrl;

    // Pour l'affichage
    private String resume;

 private List<FormationVideoDTO> videos;
private List<FormationSupportDTO> supports;
}