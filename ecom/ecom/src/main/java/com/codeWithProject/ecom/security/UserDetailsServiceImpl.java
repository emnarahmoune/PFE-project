package com.codeWithProject.ecom.security;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeRepository employeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.debug("Chargement utilisateur: {}", email);

        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + email));

        // 🚫 BLOQUER SI INACTIF
        if (!"ACTIF".equalsIgnoreCase(employe.getStatut())) {
            log.warn("Compte INACTIF bloqué: {}", email);
            throw new DisabledException("Compte désactivé");
        }

        // 🔥 NORMALISER ROLE
        String role = employe.getRole();
        if (role == null || role.isBlank()) {
            role = "USER";
        }

        role = role.replace("ROLE_", "").toUpperCase();
        String authority = "ROLE_" + role;

        return User.builder()
                .username(employe.getEmail())
                .password("") // Keycloak
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(authority)))
                .accountExpired(false)
                .accountLocked(Boolean.TRUE.equals(employe.getCompteVerrouille()))
                .credentialsExpired(false)
                .disabled(!"ACTIF".equalsIgnoreCase(employe.getStatut()))
                .build();
    }
}