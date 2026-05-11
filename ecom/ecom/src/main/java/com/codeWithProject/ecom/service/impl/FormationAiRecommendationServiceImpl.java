package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.service.FormationAiRecommendationService;
import com.codeWithProject.ecom.service.dto.FormationRecommendationRequest;
import com.codeWithProject.ecom.service.dto.FormationRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class FormationAiRecommendationServiceImpl implements FormationAiRecommendationService {

    private final RestTemplate restTemplate;

    @Value("${ia.recommendation.url:http://localhost:5000/recommend}")
    private String iaRecommendationUrl;

    @Override
    public FormationRecommendationResponse recommend(FormationRecommendationRequest request) {
        try {
            FormationRecommendationResponse response = restTemplate.postForObject(
                    iaRecommendationUrl,
                    request,
                    FormationRecommendationResponse.class
            );

            if (response == null) {
                throw new RuntimeException("Le service IA n'a retourné aucune recommandation.");
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Service IA recommandation indisponible. Lancez Flask sur le port 5000.",
                    e
            );
        }
    }
}