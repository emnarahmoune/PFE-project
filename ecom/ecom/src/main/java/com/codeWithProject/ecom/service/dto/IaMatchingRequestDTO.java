package com.codeWithProject.ecom.service.dto;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IaMatchingRequestDTO {

    private Long candidatureId;

    private Long offreId;

    private Long employeId;

    private String titrePoste;

    private String description;

    private List<String> competencesRequises;

    private List<String> technologiesRequises;

    private Integer experienceMin;

    private String niveauEtude;

    private String cvText;
}