package com.codeWithProject.ecom.security;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        log.debug("Chargement de l'utilisateur par email: {}", email);
        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec email: " + email));

        // Déterminer le rôle pour Spring Security
        String role = employe.getRole();
        if (role == null) role = "user";

        // Construire l'autorité avec le préfixe ROLE_
        String authority = "ROLE_" + (role.equals("manager") ? role : role.toUpperCase());
        // Pour manager, on veut ROLE_manager (car l'annotation hasRole('manager') attend ROLE_manager)
        if ("manager".equals(role)) {
            authority = "ROLE_manager";
        } else if ("ADMIN_RH".equals(role)) {
            authority = "ROLE_ADMIN_RH";
        } else {
            authority = "ROLE_USER";
        }

        return User.builder()
                .username(employe.getEmail())
                .password("") // mot de passe vide car géré par Keycloak
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(authority)))
                .accountExpired(false)
                .accountLocked(employe.getCompteVerrouille() != null && employe.getCompteVerrouille())
                .credentialsExpired(false)
                .disabled(employe.getActif() == null || !employe.getActif())
                .build();
    }
}