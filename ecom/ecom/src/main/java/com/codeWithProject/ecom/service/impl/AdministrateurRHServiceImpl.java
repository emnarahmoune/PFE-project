package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.UtilisateurRepository;
import com.codeWithProject.ecom.service.AdministrateurRHService;
import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.AdministrateurRHMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdministrateurRHServiceImpl implements AdministrateurRHService {

    private final AdministrateurRHRepository administrateurRHRepository;
    private final EmployeRepository          employeRepository;
    private final UtilisateurRepository      utilisateurRepository;
    private final AdministrateurRHMapper     mapper;

    // ── Lecture ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdministrateurRHDTO> findAll() {
        log.debug("Récupération de tous les administrateurs");
        return administrateurRHRepository.findAllWithDetails().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdministrateurRHDTO> findAll(Pageable pageable) {
        log.debug("Récupération des administrateurs page={}", pageable.getPageNumber());
        Page<AdministrateurRH> page = administrateurRHRepository.findAll(pageable);
        List<AdministrateurRHDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findById(Long id) {
        return administrateurRHRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findByEmployeId(Long employeId) {
        return administrateurRHRepository.findByEmployeId(employeId).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findByEmployeMatricule(String matricule) {
        return administrateurRHRepository.findByEmployeMatricule(matricule).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findByEmail(String email) {
        return administrateurRHRepository.findByEmail(email).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAdministrateur(String email) {
        return administrateurRHRepository.isEmailAdministrateur(email);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return administrateurRHRepository.countAdministrateurs();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrateurRHDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        return administrateurRHRepository.searchAdministrateurs(keyword.trim()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ── Écriture ───────────────────────────────────────────────

    @Override
    public AdministrateurRHDTO create(AdministrateurRHDTO dto) {
        log.debug("Création d'un nouvel administrateur");

        if (dto.getEmployeId() == null) {
            throw new BusinessException("L'ID de l'employé est obligatoire");
        }

        Employe employe = employeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

        if (administrateurRHRepository.existsByEmployeId(dto.getEmployeId())) {
            throw new BusinessException("Cet employé est déjà administrateur");
        }

        AdministrateurRH admin = mapper.toEntity(dto);
        admin.setEmploye(employe);

        AdministrateurRH saved = administrateurRHRepository.save(admin);
        log.info("Administrateur créé id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    public AdministrateurRHDTO update(Long id, AdministrateurRHDTO dto) {
        log.debug("Mise à jour de l'administrateur id={}", id);

        AdministrateurRH admin = administrateurRHRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur", id));

        // Mise à jour des champs hérités de Utilisateur
        if (dto.getNom()       != null) admin.setNom(dto.getNom());
        if (dto.getPrenom()    != null) admin.setPrenom(dto.getPrenom());
        if (dto.getEmail()     != null) admin.setEmail(dto.getEmail());
        if (dto.getTelephone() != null) admin.setTelephone(dto.getTelephone());
        if (dto.getActif()     != null) admin.setActif(dto.getActif());

        // Mise à jour de la relation Employe si fournie
        if (dto.getEmployeId() != null) {
            Employe employe = employeRepository.findById(dto.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));
            admin.setEmploye(employe);
        }

        AdministrateurRH saved = administrateurRHRepository.save(admin);
        log.info("Administrateur mis à jour id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    public AdministrateurRHDTO assignerEmploye(Long adminId, Long employeId) {
        log.debug("Assignation employé {} → administrateur {}", employeId, adminId);

        AdministrateurRH admin = administrateurRHRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur", adminId));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        administrateurRHRepository.findByEmployeId(employeId).ifPresent(existing -> {
            if (!existing.getId().equals(adminId)) {
                throw new BusinessException(
                        "L'employé " + employeId + " est déjà associé à un autre administrateur");
            }
        });

        admin.setEmploye(employe);
        return mapper.toDto(administrateurRHRepository.save(admin));
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression administrateur id={}", id);
        if (!administrateurRHRepository.existsById(id)) {
            throw new ResourceNotFoundException("Administrateur", id);
        }
        administrateurRHRepository.deleteById(id);
        log.info("Administrateur supprimé id={}", id);
    }

    // ── Métier (diagramme) — délégués aux services spécialisés ─

    @Override
    public void creerEmploye(AdministrateurRHDTO adminDto, Object employeDto) {
        log.debug("creerEmploye — déléguer à EmployeService");
        // À implémenter via injection de EmployeService
    }

    @Override
    public void modifierEmploye(Long employeId, Object employeDto) {
        log.debug("modifierEmploye id={}", employeId);
    }

    @Override
    public void supprimerEmploye(Long employeId) {
        log.debug("supprimerEmploye id={}", employeId);
        if (!employeRepository.existsById(employeId)) {
            throw new ResourceNotFoundException("Employé", employeId);
        }
        employeRepository.deleteById(employeId);
    }

    @Override
    public void consulterEmploye(Long employeId) {
        log.debug("consulterEmploye id={}", employeId);
    }

    @Override
    public void creerCompetence(Object competenceDto) {
        log.debug("creerCompetence — déléguer à CompetenceService");
    }

    @Override
    public void modifierCompetence(Long competenceId, Object competenceDto) {
        log.debug("modifierCompetence id={}", competenceId);
    }

    @Override
    public void supprimerCompetence(Long competenceId) {
        log.debug("supprimerCompetence id={}", competenceId);
    }

    @Override
    public void associerCompetenceEmploye(Long employeId, Long competenceId, String niveau) {
        log.debug("associerCompetenceEmploye emp={} comp={} niveau={}", employeId, competenceId, niveau);
    }

    @Override
    public void creerFormation(Object formationDto) {
        log.debug("creerFormation — déléguer à FormationService");
    }

    @Override
    public void modifierFormation(Long formationId, Object formationDto) {
        log.debug("modifierFormation id={}", formationId);
    }

    @Override
    public void supprimerFormation(Long formationId) {
        log.debug("supprimerFormation id={}", formationId);
    }

    @Override
    public void consulterDashboardsRH() {
        log.debug("consulterDashboardsRH");
    }

    @Override
    public void analyserTurnover() {
        log.debug("analyserTurnover — déléguer à SystemeBIService");
    }

    @Override
    public void analyserAbsenteisme() {
        log.debug("analyserAbsenteisme — déléguer à SystemeBIService");
    }

    @Override
    public void consulterScoresRisque() {
        log.debug("consulterScoresRisque — déléguer à SystemeBIService");
    }
}