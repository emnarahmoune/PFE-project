package com.codeWithProject.ecom.service.dto;

import java.math.BigDecimal;
import com.codeWithProject.ecom.entity.enums.StatutOffreRecrutement;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffreRecrutementResponse {

    private Long id;

    private String titrePoste;

    private String description;

    private String departement;

    private String typeContrat;

    private String localisation;

    private List<String> competencesRequises;

    private List<String> technologiesRequises;

    private Integer experienceMin;

    private String niveauEtude;

    private StatutOffreRecrutement statut;

    private LocalDateTime datePublication;

    private LocalDate dateExpiration;

    private Long creeParId;

    private String creeParNom;

    private Integer nombreCandidatures;

    private Integer meilleurScore;

private BigDecimal salairePropose;



}