package com.codeWithProject.ecom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    // 🔥 CAMUNDA
    @Bean
    @Order(0)
    public SecurityFilterChain camundaFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})                .securityMatcher("/camunda/**", "/app/**", "/api/engine/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // // 🔥 SWAGGER / ACTUATOR
    // @Bean
    // @Order(1)
    // public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    //     http
    //             .cors(cors -> {})
    //             .securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/actuator/**")
    //             .csrf(csrf -> csrf.disable())
    //             .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    //     return http.build();
    // }

    // 🔥 EMPLOYE FORMATION PUBLIC@Bean
// @Order(2)
// public SecurityFilterChain employeFormationSecure(HttpSecurity http) throws Exception {
//     http
//         .cors(cors -> {})
//         .securityMatcher("/api/employe-formations/**")
//         .csrf(csrf -> csrf.disable())
//         .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//         .authorizeHttpRequests(auth -> auth
//                 .anyRequest().authenticated()
//         )
//         .oauth2ResourceServer(oauth2 -> oauth2.jwt());

//     return http.build();
// }

    // 🔐 API SECURISEE
    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/conges/**").authenticated()
                        .requestMatchers("/api/workflow/manager/**").hasAnyRole("manager", "MANAGER")
                        .requestMatchers("/api/workflow/rh/**").hasAnyRole("admin_rh", "ADMIN_RH", "admin")
                        .requestMatchers("/api/workflow/instance/**").authenticated()
                        .requestMatchers("/api/formations/**").authenticated()
                        .requestMatchers("/api/competences/**").authenticated()
                        .requestMatchers("/api/indicateurs/**").authenticated()
                        .requestMatchers("/api/employes/**").authenticated()
                        .requestMatchers("/api/employe-formations/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    // 🔐 JWT ROLES
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
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_RH"));
                    } else if ("manager".equalsIgnoreCase(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_manager"));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    }
                }
            }
            return authorities;
        });
        return converter;
    }

    // 🔑 PASSWORD
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}