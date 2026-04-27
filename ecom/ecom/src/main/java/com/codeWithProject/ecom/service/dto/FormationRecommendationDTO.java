package com.codeWithProject.ecom.service.dto;

import com.codeWithProject.ecom.entity.Formation;

public class FormationRecommendationDTO {

    private Formation formation;
    private int score;

    public FormationRecommendationDTO(Formation formation, int score) {
        this.formation = formation;
        this.score = score;
    }

    public Formation getFormation() { return formation; }
    public int getScore() { return score; }
}