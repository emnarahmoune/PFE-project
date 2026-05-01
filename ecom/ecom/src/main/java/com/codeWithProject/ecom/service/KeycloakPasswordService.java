package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakPasswordService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.frontend-client-id}")
    private String frontendClientId;

    @Value("${keycloak.backend-client-id}")
    private String backendClientId;

    @Value("${keycloak.backend-client-secret}")
    private String backendClientSecret;

    public void changePassword(String email, String oldPassword, String newPassword) {
        verifyOldPassword(email, oldPassword);

        String adminToken = getAdminToken();
        String userId = findUserIdByEmail(adminToken, email);

        resetPassword(adminToken, userId, newPassword);
    }

    private void verifyOldPassword(String email, String oldPassword) {
        String url = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", frontendClientId);
        body.add("username", email);
        body.add("password", oldPassword);
        body.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpClientErrorException e) {
            throw new BusinessException("Ancien mot de passe incorrect");
        }
    }

    private String getAdminToken() {
        String url = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", backendClientId);
        body.add("client_secret", backendClientSecret);
        body.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Object accessToken = response.getBody() != null ? response.getBody().get("access_token") : null;

        if (accessToken == null) {
            throw new BusinessException("Impossible d'obtenir le token admin Keycloak");
        }

        return accessToken.toString();
    }

    private String findUserIdByEmail(String adminToken, String email) {
        String url = keycloakUrl + "/admin/realms/" + realm + "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        List users = response.getBody();

        if (users == null || users.isEmpty()) {
            throw new BusinessException("Utilisateur Keycloak introuvable");
        }

        Map user = (Map) users.get(0);

        Object id = user.get("id");

        if (id == null) {
            throw new BusinessException("ID utilisateur Keycloak introuvable");
        }

        return id.toString();
    }

    private void resetPassword(String adminToken, String userId, String newPassword) {
        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Void.class
        );
    }
}