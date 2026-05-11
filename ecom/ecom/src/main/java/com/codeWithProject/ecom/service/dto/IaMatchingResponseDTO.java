package com.codeWithProject.ecom.service.dto;


import com.codeWithProject.ecom.entity.enums.NiveauCompatibilite;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IaMatchingResponseDTO {

    private String texteExtrait;

    private List<String> competencesDetectees;

    private List<String> technologiesDetectees;

    private List<String> experiencesDetectees;

    private Integer anneesExperienceEstimees;

    private String resumeProfil;

    private List<String> pointsForts;

    private List<String> pointsFaibles;

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
}