package com.codeWithProject.ecom.service.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequest {

    private String mode;

    private String poste;

    private Map<String, Integer> userSkills;

    private Map<String, Integer> requiredSkills;

    private List<AiFormationDto> formations;

    private List<String> formationsSuivies;
}