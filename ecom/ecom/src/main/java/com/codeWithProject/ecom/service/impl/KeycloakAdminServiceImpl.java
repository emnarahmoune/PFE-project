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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final RestTemplate restTemplate;

    @Value("${keycloak.admin.server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:portail_rh}")
    private String realm;


  
    /*
     * Client utilisé pour obtenir un token ADMIN via client_credentials.
     * Ce client doit être confidential + service accounts enabled
     * et avoir le rôle realm-management/manage-users.
     */
    @Value("${keycloak.admin.client-id:portail_rh_backend}")
    private String adminClientId;

    @Value("${keycloak.admin.client-secret:}")
    private String adminClientSecret;

    /*
     * Client utilisé pour vérifier l'ancien mot de passe via password grant.
     * Il peut être le même client si Direct Access Grants est activé.
     */
    @Value("${keycloak.user.client-id:${keycloak.admin.client-id:portail_rh_backend}}")
    private String userClientId;

    @Value("${keycloak.user.client-secret:${keycloak.admin.client-secret:}}")
    private String userClientSecret;
@Override
public boolean verifyPassword(String usernameOrEmail, String rawPassword) {
    if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
        return false;
    }

    if (rawPassword == null || rawPassword.isBlank()) {
        return false;
    }

    String tokenUrl = cleanBaseUrl(keycloakServerUrl)
            + "/realms/" + realm
            + "/protocol/openid-connect/token";

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", userClientId);
    form.add("username", usernameOrEmail.trim());
    form.add("password", rawPassword);

    if (userClientSecret != null && !userClientSecret.isBlank()) {
        form.add("client_secret", userClientSecret);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request =
            new HttpEntity<>(form, headers);

    try {
        log.info(
                "VERIFY PASSWORD KEYCLOAK => url={}, realm={}, clientId={}, username={}",
                tokenUrl,
                realm,
                userClientId,
                usernameOrEmail
        );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(tokenUrl, request, Map.class);

        return response.getStatusCode().is2xxSuccessful()
                && response.getBody() != null
                && response.getBody().get("access_token") != null;

    } catch (org.springframework.web.client.HttpStatusCodeException e) {
        log.warn(
                "VERIFY PASSWORD FAILED => status={}, body={}, clientId={}, username={}",
                e.getStatusCode(),
                e.getResponseBodyAsString(),
                userClientId,
                usernameOrEmail
        );

        return false;

    } catch (Exception e) {
        log.warn(
                "VERIFY PASSWORD ERROR => clientId={}, username={}, error={}",
                userClientId,
                usernameOrEmail,
                e.getMessage()
        );

        return false;
    }
}

    @Override
    public void resetPassword(String keycloakUserId, String newPassword) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new BusinessException("ID utilisateur Keycloak introuvable");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException("Nouveau mot de passe obligatoire");
        }

        String adminToken = getAdminAccessToken();

        String url = cleanBaseUrl(keycloakServerUrl)
                + "/admin/realms/" + realm
                + "/users/" + keycloakUserId
                + "/reset-password";

        Map<String, Object> body = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    Void.class
            );

            log.info(
                    "Mot de passe Keycloak modifié avec succès pour userId={}",
                    keycloakUserId
            );

        } catch (HttpStatusCodeException e) {
            log.error(
                    "Erreur reset password Keycloak : status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new BusinessException(
                    "Impossible de changer le mot de passe dans Keycloak : "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            log.error("Erreur inattendue reset password Keycloak", e);

            throw new BusinessException(
                    "Impossible de changer le mot de passe dans Keycloak : "
                            + e.getMessage()
            );
        }
    }

    private String getAdminAccessToken() {
        if (adminClientSecret == null || adminClientSecret.isBlank()) {
            throw new BusinessException(
                    "Client secret Keycloak manquant pour le client admin : " + adminClientId
            );
        }

        String tokenUrl = cleanBaseUrl(keycloakServerUrl)
                + "/realms/" + realm
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null
                    || response.getBody().get("access_token") == null) {
                throw new BusinessException("Impossible de récupérer le token admin Keycloak");
            }

            return response.getBody().get("access_token").toString();

        } catch (HttpStatusCodeException e) {
            log.error(
                    "Erreur token admin Keycloak : status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new BusinessException(
                    "Erreur token admin Keycloak : "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            log.error("Erreur inattendue récupération token admin Keycloak", e);

            throw new BusinessException(
                    "Erreur token admin Keycloak : " + e.getMessage()
            );
        }
    }

    private String cleanBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException("URL Keycloak manquante");
        }

        String cleaned = url.trim();

        if (cleaned.endsWith("/")) {
            return cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned;
    }
}