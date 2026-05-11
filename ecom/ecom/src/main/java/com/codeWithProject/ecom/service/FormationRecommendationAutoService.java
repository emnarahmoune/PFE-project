package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Employe;

public interface FormationRecommendationAutoService {

    void generateBoostRecommendationsForEmploye(Employe employe);

    void generateGapRecommendationsForEmployePoste(Employe employe);

    void generateAllRecommendationsForEmploye(Employe employe);
}