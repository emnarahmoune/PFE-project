package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.FormationRecommendationRequest;
import com.codeWithProject.ecom.service.dto.FormationRecommendationResponse;

public interface FormationAiRecommendationService {

    FormationRecommendationResponse recommend(FormationRecommendationRequest request);
}