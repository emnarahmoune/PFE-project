package com.codeWithProject.ecom.security;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthFilter extends OncePerRequestFilter {

    private final EmployeRepository employeRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = extractEmail(authentication);

        if (email == null || email.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Employe employe = employeRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (employe == null) {
            log.debug("Aucun employé trouvé pour email: {}", email);
            filterChain.doFilter(request, response);
            return;
        }

        log.info(
                "FILTER CHECK => path={}, method={}, email={}, statut={}, actif={}",
                path,
                method,
                email,
                employe.getStatut(),
                employe.getActif()
        );

        boolean statutActif = "ACTIF".equalsIgnoreCase(employe.getStatut());
        boolean compteActif = employe.getActif() == null || Boolean.TRUE.equals(employe.getActif());

        if (!statutActif || !compteActif) {
            log.warn("Accès refusé utilisateur INACTIF: {}", email);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Compte désactivé\"}");

            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();

            String email = jwt.getClaimAsString("email");

            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("preferred_username");
            }

            if (email == null || email.isBlank()) {
                email = jwt.getSubject();
            }

            return email;
        }

        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");

            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("preferred_username");
            }

            if (email == null || email.isBlank()) {
                email = jwt.getSubject();
            }

            return email;
        }

        return authentication.getName();
    }
}