package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.ManagerProfileService;
import com.codeWithProject.ecom.service.dto.ManagerProfileDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ManagerProfileServiceImpl implements ManagerProfileService {

    private final ManagerRepository managerRepository;
    private final EmployeRepository employeRepository;

    @Override
    @Transactional(readOnly = true)
    public ManagerProfileDTO getProfile(String email) {
        log.debug("Récupération du profil manager pour l'email : {}", email);
        Manager manager = findManagerByEmail(email);
        return convertToDto(manager);
    }

    @Override
    public ManagerProfileDTO updateProfile(String email, ManagerProfileDTO dto) {
        log.debug("Mise à jour du profil manager pour l'email : {}", email);
        Manager manager = findManagerByEmail(email);

        if (!manager.getEmail().equalsIgnoreCase(dto.getEmail())) {
            employeRepository.findByEmail(dto.getEmail())
                    .filter(other -> !other.getId().equals(manager.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Cet email est déjà utilisé par un autre employé");
                    });
        }

        manager.setNom(dto.getNom());
        manager.setPrenom(dto.getPrenom());
        manager.setEmail(dto.getEmail().toLowerCase());
        manager.setTelephone(dto.getTelephone());
        manager.setPoste(dto.getPoste());
        manager.setDepartement(dto.getDepartement());

        Manager saved = managerRepository.save(manager);
        log.info("Profil manager mis à jour : {}", saved.getEmail());
        return convertToDto(saved);
    }

    private Manager findManagerByEmail(String email) {
        return managerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", "email", email));
    }

    private ManagerProfileDTO convertToDto(Manager manager) {
        long anciennete = (manager.getDateEmbauche() != null)
                ? ChronoUnit.YEARS.between(manager.getDateEmbauche(), LocalDate.now())
                : 0L;

        long ancienneteManager = (manager.getDateNomination() != null)
                ? ChronoUnit.MONTHS.between(manager.getDateNomination(), LocalDate.now())
                : 0L;

        int nombreEmployesGeres = (manager.getEmployesGeres() != null)
                ? manager.getEmployesGeres().size()
                : 0;

        return ManagerProfileDTO.builder()
                .id(manager.getId())
                .matricule(manager.getMatricule())
                .nom(manager.getNom())
                .prenom(manager.getPrenom())
                .email(manager.getEmail())
                .telephone(manager.getTelephone())
                .poste(manager.getPoste())
                .departement(manager.getDepartement())
                .photoUrl(manager.getPhotoUrl())          // ← AJOUT CRUCIAL
                .dateEmbauche(manager.getDateEmbauche())
                .dateNomination(manager.getDateNomination())
                .actif(manager.getActif())
                .role(manager.getRole())
                .soldeConges(manager.getSoldeConges())
                .statutCompte(manager.getStatutCompte())
                .anciennete(anciennete)
                .ancienneteManager(ancienneteManager)
                .nombreEmployesGeres(nombreEmployesGeres)
                .nomComplet(manager.getNomComplet())
                .build();
    }
}