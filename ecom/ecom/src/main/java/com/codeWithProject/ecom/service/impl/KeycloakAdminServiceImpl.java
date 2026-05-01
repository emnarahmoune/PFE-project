package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.service.KeycloakAdminService;
import com.codeWithProject.ecom.service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    @Value("${keycloak.admin.server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:portail_rh}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-backend-client}")
    private String clientId;

    @Value("${keycloak.admin.client-secret:}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    private String getAdminAccessToken() {
        // Log pour vérifier les valeurs chargées (cacher la fin du secret)
        String maskedSecret = clientSecret != null && clientSecret.length() > 6
                ? clientSecret.substring(0, 6) + "..." : "null";
        log.info("Keycloak Admin config: url={}, realm={}, clientId={}, clientSecret={}",
                keycloakServerUrl, realm, clientId, maskedSecret);

        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Utilisation de MultiValueMap pour un encodage correct
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
            throw new BusinessException("Impossible d'obtenir le token admin Keycloak : réponse sans access_token");
        } catch (Exception e) {
            log.error("Erreur lors de l'obtention du token admin Keycloak", e);
            throw new BusinessException("Erreur de communication avec Keycloak: " + e.getMessage());
        }
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
        String token = getAdminAccessToken();

        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("type", "password");
        body.put("value", newPassword);
        body.put("temporary", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Mot de passe Keycloak mis à jour pour userId : {}", userId);
            } else {
                throw new BusinessException("Échec de la mise à jour du mot de passe Keycloak, status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Erreur lors du reset du mot de passe Keycloak", e);
            throw new BusinessException("Impossible de changer le mot de passe via Keycloak: " + e.getMessage());
        }
    }
}