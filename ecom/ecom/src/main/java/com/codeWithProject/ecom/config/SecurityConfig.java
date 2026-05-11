package com.codeWithProject.ecom.config;

import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.security.KeycloakAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final EmployeRepository employeRepository;

    @Bean
    @Order(0)
    public SecurityFilterChain camundaFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/camunda/**", "/app/**", "/api/engine/**")
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // =========================
                        // Public / Auth
                        // =========================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/uploads/**").permitAll()
                        .requestMatchers("/api/photos/**").permitAll()

                        // =========================
                        // Profil employé connecté
                        // =========================
                        .requestMatchers(HttpMethod.GET, "/api/employes/mon-profil").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/employes/mon-profil").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/employes/mon-profil").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/employes/mon-profil/photo").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/employes/mon-profil/photo").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employes/mon-solde-conges").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employes/mes-competences").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employes/mes-formations").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employes/mon-historique-conges").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/employes/change-password").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/employes/change-email").authenticated()
                        .requestMatchers("/api/employes/me/competences/**").authenticated()

                        // =========================
                        // Audit RH réservé Admin/RH
                        // =========================
                        .requestMatchers("/api/audit-logs/**").hasAnyAuthority(
                                "ROLE_ADMIN_RH",
                                "ROLE_admin_rh",
                                "ROLE_ADMIN",
                                "ADMIN_RH",
                                "admin_rh",
                                "admin"
                        )

                        // =========================
                        // Congés
                        // =========================
                        .requestMatchers("/api/conges/**").authenticated()

                        // =========================
                        // Workflow manager
                        // =========================
                        .requestMatchers("/api/workflow/manager/**").hasAnyAuthority(
                                "ROLE_MANAGER",
                                "ROLE_manager",
                                "MANAGER",
                                "manager"
                        )

                        // =========================
                        // Workflow RH
                        // =========================
                        .requestMatchers("/api/workflow/rh/**").hasAnyAuthority(
                                "ROLE_ADMIN_RH",
                                "ROLE_admin_rh",
                                "ROLE_ADMIN",
                                "ADMIN_RH",
                                "admin_rh",
                                "admin"
                        )

                        .requestMatchers("/api/workflow/instance/**").authenticated()

                        // =========================
                        // Modules authentifiés
                        // =========================
                        .requestMatchers("/api/formations/**").authenticated()
                        .requestMatchers("/api/competences/**").authenticated()
                        .requestMatchers("/api/indicateurs/**").authenticated()
                        .requestMatchers("/api/employe-formations/**").authenticated()

                        // =========================
                        // Employés
                        // =========================
                        .requestMatchers(HttpMethod.GET, "/api/employes/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/employes/**").hasAnyAuthority(
                                "ROLE_ADMIN_RH",
                                "ROLE_admin_rh",
                                "ROLE_ADMIN",
                                "ADMIN_RH",
                                "admin_rh",
                                "admin"
                        )

                        .requestMatchers(HttpMethod.PUT, "/api/employes/**").hasAnyAuthority(
                                "ROLE_ADMIN_RH",
                                "ROLE_admin_rh",
                                "ROLE_ADMIN",
                                "ADMIN_RH",
                                "admin_rh",
                                "admin"
                        )

                        .requestMatchers(HttpMethod.DELETE, "/api/employes/**").hasAnyAuthority(
                                "ROLE_ADMIN_RH",
                                "ROLE_admin_rh",
                                "ROLE_ADMIN",
                                "ADMIN_RH",
                                "admin_rh",
                                "admin"
                        )

                        .requestMatchers(HttpMethod.PATCH, "/api/employes/**").authenticated()

                        // =========================
                        // Recrutement interne
                        // Temporairement authentifié pour débloquer POST/PUT/PATCH/DELETE
                        // =========================
                        .requestMatchers("/api/recrutement/**").authenticated()
                        .requestMatchers("/api/candidatures/**").authenticated()
                        .requestMatchers("/api/cv-analysis/**").authenticated()

                        // IMPORTANT : toujours en dernier
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .addFilterAfter(
                        new KeycloakAuthFilter(employeRepository),
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");

                for (String role : roles) {
                    if ("admin".equalsIgnoreCase(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_RH"));

                    } else if ("admin_rh".equalsIgnoreCase(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_RH"));

                    } else if ("manager".equalsIgnoreCase(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));

                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    }
                }
            }

            return authorities;
        });

        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}