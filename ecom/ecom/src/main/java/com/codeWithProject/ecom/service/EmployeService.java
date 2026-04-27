package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.EmployeCompetence;
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


private String convertToLevel(int niveau) {
    return switch (niveau) {
        case 1 -> "DEBUTANT";
        case 2 -> "INTERMEDIAIRE";
        case 3 -> "AVANCE";
        case 4, 5 -> "EXPERT";
        default -> "DEBUTANT";
    };
}

void addCompetence(Long userId, Long compId, int niveau);

    Optional<EmployeDTO> findById(Long id);

    Optional<EmployeDTO> findByMatricule(String matricule);

    List<EmployeDTO> findByDepartement(String departement);

    List<EmployeDTO> findByStatut(String statut);

    List<EmployeDTO> findByManagerId(Long managerId);

    List<EmployeDTO> findByServiceId(Long serviceId);

    List<EmployeDTO> findActifs();

    List<EmployeDTO> findSoldeCongesFaible(Integer seuil);

    // ===== MÉTHODES CRUD =====

    EmployeDTO create(EmployeDTO dto);

    EmployeDTO update(Long id, EmployeDTO dto);

    EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement);

    EmployeDTO changerStatut(Long id, String nouveauStatut);

    void delete(Long id);

    // ===== MÉTHODES STATISTIQUES =====

    Map<String, Long> countByDepartement();

    Map<String, Long> countByStatut();

    Double calculerMasseSalariale();

    Double calculerSalaireMoyen();

    List<EmployeDTO> findEmployesRecents(int limit);

    List<EmployeDTO> search(String keyword);

    /**
     * Récupère les statistiques pour le tableau de bord
     */
    TableauBordEmployeDTO getStatsTableauBord();

    // ===== MÉTHODES POUR L'UTILISATEUR AUTHENTIFIÉ =====

    /**
     * Trouve un employé par son email
     */
    Optional<EmployeDTO> findByEmail(String email);

    /**
     * Récupère le solde de congés d'un employé par son email
     */
    SoldeCongesDTO getSoldeCongesByEmail(String email);

    /**
     * Récupère les compétences d'un employé par son email
     */
    List<CompetenceEmployeDTO> getCompetencesByEmail(String email);

    /**
     * Récupère les formations d'un employé par son email
     */
    List<FormationEmployeDTO> getFormationsByEmail(String email);

    /**
     * Récupère l'historique des congés d'un employé par son email
     */
    List<HistoriqueCongeDTO> getHistoriqueCongesByEmail(String email);

    /**
     * Met à jour le profil d'un employé par son email
     */
    EmployeDTO updateProfilByEmail(String email, UpdateProfilRequest request);

    /**
     * Change le mot de passe d'un employé par son email
     */
    void changePasswordByEmail(String email, ChangePasswordRequest request);


    public List<EmployeCompetence> getCompetencesEntity(Long userId) ;
    /**
     * Change l'email d'un employé
     */
    EmployeDTO changeEmailByEmail(String email, String newEmail);

Long getEmployeIdByEmail(String email);
    // ===== NOUVELLE MÉTHODE : MISE À JOUR DU MANAGER =====

    /**
     * Met à jour le manager d'un employé
     * @param employeId ID de l'employé
     * @param managerId ID du nouveau manager (peut être null pour supprimer le manager)
     * @return L'employé mis à jour
     */
    EmployeDTO updateManager(Long employeId, Long managerId);
    public void updateCompetences(Long employeId, List<CompetenceDTO> dtos);
}