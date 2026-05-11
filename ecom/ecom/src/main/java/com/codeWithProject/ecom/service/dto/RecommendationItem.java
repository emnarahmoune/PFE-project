package com.codeWithProject.ecom.service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationItem {

    private Long formationId;

    private String formation;

    private String title;

    private String provider;

    private String url;

    private String description;

    private String level;

    private Double score;

    private Double semanticScore;

    private Double skillScore;

    private Double priorityScore;

    private String type;

    private List<String> matchedSkills;

    private String reason;

    private List<RecommendedVideoDto> videos;
}