package com.codeWithProject.ecom.service;

public interface KeycloakAdminService {

    /**
     * Change le mot de passe d'un utilisateur Keycloak via son ID
     * @param userId l'identifiant Keycloak de l'utilisateur
     * @param newPassword nouveau mot de passe
     */
    void resetPassword(String userId, String newPassword);
}