package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import com.codeWithProject.ecom.service.dto.CompetenceDTO;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interface service AdministrateurRH
 */
public interface AdministrateurRHService {

    // ── Lecture ────────────────────────────────────────────────

    List<AdministrateurRHDTO> findAll();
    Page<AdministrateurRHDTO> findAll(Pageable pageable);
    Optional<AdministrateurRHDTO> findById(Long id);
    Optional<AdministrateurRHDTO> findByEmployeId(Long employeId);
    Optional<AdministrateurRHDTO> findByEmployeMatricule(String matricule);
    Optional<AdministrateurRHDTO> findByEmail(String email);
    boolean isEmailAdministrateur(String email);
    long count();
    List<AdministrateurRHDTO> search(String keyword);

    // ── Écriture ───────────────────────────────────────────────

    AdministrateurRHDTO create(AdministrateurRHDTO dto);
    AdministrateurRHDTO update(Long id, AdministrateurRHDTO dto);
    AdministrateurRHDTO assignerEmploye(Long adminId, Long employeId);
    void delete(Long id);

    // ── Métier (diagramme de classes) ──────────────────────────

    // Gestion des employés
    void creerEmploye(EmployeDTO employeDto);
    void modifierEmploye(Long employeId, EmployeDTO employeDto);
    void supprimerEmploye(Long employeId);
    void consulterEmploye(Long employeId);

    // Gestion des compétences
    void creerCompetence(CompetenceDTO competenceDto);
    void modifierCompetence(Long competenceId, CompetenceDTO competenceDto);
    void supprimerCompetence(Long competenceId);
    void associerCompetenceEmploye(Long employeId, Long competenceId, String niveau);

    // Gestion des formations
    void creerFormation(FormationDTO formationDto);
    void modifierFormation(Long formationId, FormationDTO formationDto);
    void supprimerFormation(Long formationId);

    // Reporting
    void consulterDashboardsRH();
    void analyserTurnover();
    void analyserAbsenteisme();
    void consulterScoresRisque();
}