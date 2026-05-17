package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.service.FormationAiRecommendationService;
import com.codeWithProject.ecom.service.dto.FormationRecommendationRequest;
import com.codeWithProject.ecom.service.dto.FormationRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class FormationAiRecommendationServiceImpl implements FormationAiRecommendationService {

    private final RestTemplate restTemplate;

 @Value("${ai.recommendation.url}")
private String aiRecommendationUrl;

    @Override
    public FormationRecommendationResponse recommend(FormationRecommendationRequest request) {
        try {
           FormationRecommendationResponse response = restTemplate.postForObject(
        aiRecommendationUrl + "/recommend",
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