package com.codeWithProject.ecom.service.dto;

import com.codeWithProject.ecom.entity.enums.NiveauCompatibilite;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecrutementScoreResponse {

    private Long id;

    private Long candidatureId;

    private Long offreId;

    private Long employeId;

    private Integer scoreGlobal;

    private Integer scoreCompetences;

    private Integer scoreTechnologies;

    private Integer scoreExperience;

    private Integer scoreFormation;

    private NiveauCompatibilite niveauCompatibilite;

    private List<String> competencesCorrespondantes;

    private List<String> competencesManquantes;

    private List<String> technologiesCorrespondantes;

    private List<String> technologiesManquantes;

    private String justificationIa;

    private String recommandationIa;

    private LocalDateTime dateCalcul;
}