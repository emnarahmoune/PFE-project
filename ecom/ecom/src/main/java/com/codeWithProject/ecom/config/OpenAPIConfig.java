package com.codeWithProject.ecom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration d'OpenAPI/Swagger pour la documentation de l'API RH
 * Portail RH avec authentification Keycloak
 */
@Configuration
public class OpenAPIConfig {

    @Value("${server.port:8082}")
    private int port;

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm:portail_rh}")
    private String realm;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Portail RH - Gestion des Ressources Humaines")
                        .description("""
                                API REST complète pour la gestion des ressources humaines du Portail RH.
                                
                                ## 🏢 Fonctionnalités principales
                                * Gestion des employés, managers et administrateurs
                                * Gestion des compétences et formations
                                * Gestion des demandes de congé avec workflow
                                * Indicateurs RH et tableaux de bord
                                * Prédiction des risques de turnover
                                
                                ## 🔐 Authentification avec Keycloak
                                Cette API utilise **Keycloak** avec OAuth2 et JWT pour l'authentification et l'autorisation.
                                
                                ### Rôles disponibles
                                * **admin** - Accès complet à toutes les fonctionnalités
                                * **manager** - Gestion de son équipe
                                * **user** - Accès utilisateur standard
                                
                                ### Authentification
                                1. Obtenez un token depuis Keycloak
                                2. Utilisez le token dans l'en-tête `Authorization: Bearer <token>`
                                
                                ### URL Keycloak
                                * **Auth Server**: %s
                                * **Realm**: %s
                                * **Token Endpoint**: %s/realms/%s/protocol/openid-connect/token
                                """.formatted(keycloakUrl, realm, keycloakUrl, realm))
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Portail RH - Projet PFE")
                                .email("contact@portail-rh.com")
                                .url("https://github.com/portail-rh"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + port)
                                .description("Serveur de développement - Port " + port),
                        new Server()
                                .url("http://localhost:8082")
                                .description("API Backend - Port 8082"),
                        new Server()
                                .url("https://api.portail-rh.com")
                                .description("Serveur de production (à venir)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Keycloak"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Keycloak", new SecurityScheme()
                                .name("Keycloak")
                                .type(SecurityScheme.Type.OAUTH2)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .flows(new io.swagger.v3.oas.models.security.OAuthFlows()
                                        .password(new io.swagger.v3.oas.models.security.OAuthFlow()
                                                .tokenUrl(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                                                .refreshUrl(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                                                .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                        .addString("openid", "OpenID Connect")
                                                        .addString("profile", "Accès au profil")
                                                        .addString("email", "Accès à l'email")
                                                )
                                        )
                                )
                        )
                );
    }
}