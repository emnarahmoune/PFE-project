package com.codeWithProject.ecom.service.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationRecommendationResponse {

    private String mode;

    private String poste;

    private Map<String, Object> gapSkills;

    private Map<String, Integer> boostSkills;

    private List<RecommendationItem> recommendations;

    private String message;

    
}