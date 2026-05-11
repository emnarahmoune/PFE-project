package com.codeWithProject.ecom.service.dto;
import lombok.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffreRecrutementRequest {

    private String titrePoste;

    private String description;

    private String departement;

    private String typeContrat;

    private String localisation;

    private List<String> competencesRequises;

    private List<String> technologiesRequises;

    private Integer experienceMin;

    private String niveauEtude;

    private LocalDate dateExpiration;

private BigDecimal salairePropose;

}