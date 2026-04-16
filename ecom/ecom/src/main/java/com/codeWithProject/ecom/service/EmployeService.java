package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EmployeService {

    // ===== MÉTHODES DE BASE =====
    List<EmployeDTO> findAll();
    Page<EmployeDTO> findAll(Pageable pageable);
    long count();
    Optional<EmployeDTO> findById(Long id);
    Optional<EmployeDTO> findByMatricule(String matricule);
    List<EmployeDTO> findByDepartement(String departement);
    List<EmployeDTO> findByStatut(String statut);
    List<EmployeDTO> findByManagerId(Long managerId);
    List<EmployeDTO> findByServiceId(Long serviceId);
    List<EmployeDTO> findActifs();
    List<EmployeDTO> findSoldeCongesFaible(Integer seuil);

    // ===== CRUD =====
    EmployeDTO create(EmployeDTO dto);
    EmployeDTO update(Long id, EmployeDTO dto);
    EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement);
    EmployeDTO changerStatut(Long id, String nouveauStatut);
    void delete(Long id);

    // ===== STATISTIQUES =====
    Map<String, Long> countByDepartement();
    Map<String, Long> countByStatut();
    Double calculerMasseSalariale();
    Double calculerSalaireMoyen();
    List<EmployeDTO> findEmployesRecents(int limit);
    List<EmployeDTO> search(String keyword);
    TableauBordEmployeDTO getStatsTableauBord();

    // ===== MÉTHODES POUR L'UTILISATEUR AUTHENTIFIÉ =====
    Optional<EmployeDTO> findByEmail(String email);
    SoldeCongesDTO getSoldeCongesByEmail(String email);
    List<CompetenceEmployeDTO> getCompetencesByEmail(String email);
    List<FormationEmployeDTO> getFormationsByEmail(String email);
    List<HistoriqueCongeDTO> getHistoriqueCongesByEmail(String email);
    EmployeDTO updateProfilByEmail(String email, UpdateProfilRequest request);
    void changePasswordByEmail(String email, ChangePasswordRequest request);
    EmployeDTO changeEmailByEmail(String email, String newEmail);

    // ===== MÉTHODES POUR MANAGER =====
    List<EmployeDTO> findAllManagers();
    List<EmployeDTO> getEquipeByManagerEmail(String managerEmail);
    EmployeDTO getEmployeForManager(Long employeId, String managerEmail);
    EmployeDTO updateManager(Long employeId, Long managerId);
}