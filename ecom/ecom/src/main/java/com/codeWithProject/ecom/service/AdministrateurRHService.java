package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interface service AdministrateurRH
 *
 * Méthodes alignées avec :
 *  - le diagramme de classes (creerEmploye, modifierEmploye…)
 *  - le controller REST (findAll, findById, create, delete, search…)
 *  - l'implémentation AdministrateurRHServiceImpl
 */
public interface AdministrateurRHService {

    // ── Lecture ────────────────────────────────────────────────

    List<AdministrateurRHDTO>     findAll();
    Page<AdministrateurRHDTO>     findAll(Pageable pageable);
    Optional<AdministrateurRHDTO> findById(Long id);
    Optional<AdministrateurRHDTO> findByEmployeId(Long employeId);
    Optional<AdministrateurRHDTO> findByEmployeMatricule(String matricule);
    Optional<AdministrateurRHDTO> findByEmail(String email);
    boolean                       isEmailAdministrateur(String email);
    long                          count();
    List<AdministrateurRHDTO>     search(String keyword);

    // ── Écriture ───────────────────────────────────────────────

    AdministrateurRHDTO           create(AdministrateurRHDTO dto);
    AdministrateurRHDTO           update(Long id, AdministrateurRHDTO dto);
    AdministrateurRHDTO           assignerEmploye(Long adminId, Long employeId);
    void                          delete(Long id);

    // ── Métier (diagramme de classes) ──────────────────────────

    // Gestion des employés
    void creerEmploye(AdministrateurRHDTO adminDto, Object employeDto);
    void modifierEmploye(Long employeId, Object employeDto);
    void supprimerEmploye(Long employeId);
    void consulterEmploye(Long employeId);

    // Gestion des compétences
    void creerCompetence(Object competenceDto);
    void modifierCompetence(Long competenceId, Object competenceDto);
    void supprimerCompetence(Long competenceId);
    void associerCompetenceEmploye(Long employeId, Long competenceId, String niveau);

    // Gestion des formations
    void creerFormation(Object formationDto);
    void modifierFormation(Long formationId, Object formationDto);
    void supprimerFormation(Long formationId);

    // Reporting
    void consulterDashboardsRH();
    void analyserTurnover();
    void analyserAbsenteisme();
    void consulterScoresRisque();
}