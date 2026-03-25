package com.codeWithProject.ecom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration d'OpenAPI/Swagger pour la documentation de l'API RH
 */
@Configuration
public class OpenAPIConfig {

    @Value("${server.port:8080}")
    private int port;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestion des Ressources Humaines")
                        .description("""
                                API REST complète pour la gestion des ressources humaines.
                                
                                ## Fonctionnalités principales
                                * Gestion des employés, managers et administrateurs
                                * Gestion des compétences et formations
                                * Gestion des demandes de congé avec workflow
                                * Indicateurs RH et tableaux de bord
                                * Prédiction des risques de turnover
                                
                                ## Authentification
                                Pour l'instant, l'API utilise Spring Security avec un compte par défaut.
                                * **user** / (mot de passe généré dans les logs)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Système RH - Projet PFE")
                                .email("contact@rh-platform.com")
                                .url("https://github.com/rh-platform"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + port)
                                .description("Serveur de développement - Port dynamique"),
                        new Server()
                                .url("https://api.rh-platform.com")
                                .description("Serveur de production (à venir)")
                ));
    }
}