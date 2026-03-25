package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des employés
 */
public interface EmployeService {

    // ===== MÉTHODES EXISTANTES =====

    /**
     * Récupère tous les employés
     */
    List<EmployeDTO> findAll();

    /**
     * Récupère tous les employés avec pagination
     */
    Page<EmployeDTO> findAll(Pageable pageable);

    /**
     * Récupère un employé par son ID
     */
    Optional<EmployeDTO> findById(Long id);

    /**
     * Récupère un employé par son matricule
     */
    Optional<EmployeDTO> findByMatricule(String matricule);

    /**
     * Récupère les employés par département
     */
    List<EmployeDTO> findByDepartement(String departement);

    /**
     * Récupère les employés par statut
     */
    List<EmployeDTO> findByStatut(String statut);

    /**
     * Récupère les employés par manager
     */
    List<EmployeDTO> findByManagerId(Long managerId);

    /**
     * Récupère les employés par service
     */
    List<EmployeDTO> findByServiceId(Long serviceId);

    /**
     * Récupère les employés actifs
     */
    List<EmployeDTO> findActifs();

    /**
     * Récupère les employés avec solde de congés faible
     */
    List<EmployeDTO> findSoldeCongesFaible(Integer seuil);

    /**
     * Crée un nouvel employé
     */
    EmployeDTO create(EmployeDTO dto);

    /**
     * Met à jour un employé
     */
    EmployeDTO update(Long id, EmployeDTO dto);

    /**
     * Met à jour le profil d'un employé
     */
    EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement);

    /**
     * Change le statut d'un employé
     */
    EmployeDTO changerStatut(Long id, String nouveauStatut);

    /**
     * Supprime un employé (désactive)
     */
    void delete(Long id);

    /**
     * Compte le nombre d'employés par département
     */
    Map<String, Long> countByDepartement();

    /**
     * Compte le nombre d'employés par statut
     */
    Map<String, Long> countByStatut();

    /**
     * Calcule la masse salariale totale
     */
    Double calculerMasseSalariale();

    /**
     * Calcule le salaire moyen
     */
    Double calculerSalaireMoyen();

    /**
     * Compte le nombre total d'employés
     */
    long count();

    /**
     * Récupère les employés récents
     */
    List<EmployeDTO> findEmployesRecents(int limit);

    /**
     * Recherche des employés par mot-clé
     */
    List<EmployeDTO> search(String keyword);

    /**
     * Récupère les statistiques globales
     */
    Map<String, Object> getStatsTableauBord();

    // ===== NOUVELLES MÉTHODES POUR L'ESPACE EMPLOYÉ =====

    /**
     * Récupère un employé par son email
     */
    Optional<EmployeDTO> findByEmail(String email);

    /**
     * Récupère le solde de congés d'un employé
     */
    SoldeCongesDTO getSoldeCongesByEmail(String email);

    /**
     * Récupère les compétences d'un employé
     */
    List<CompetenceEmployeDTO> getCompetencesByEmail(String email);

    /**
     * Récupère les formations d'un employé
     */
    List<FormationEmployeDTO> getFormationsByEmail(String email);

    /**
     * Récupère l'historique des congés d'un employé
     */
    List<HistoriqueCongeDTO> getHistoriqueCongesByEmail(String email);

    /**
     * Met à jour le profil d'un employé par email
     */
    EmployeDTO updateProfilByEmail(String email, UpdateProfilRequest request);

    /**
     * Change le mot de passe d'un employé
     */
    void changePasswordByEmail(String email, ChangePasswordRequest request);

    /**
     * Change l'email d'un employé
     */
    EmployeDTO changeEmailByEmail(String email, String newEmail);
}