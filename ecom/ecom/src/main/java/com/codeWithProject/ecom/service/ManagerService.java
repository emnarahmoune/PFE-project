package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.ManagerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des managers
 */
public interface ManagerService {

    /**
     * Récupère tous les managers
     */
    List<ManagerDTO> findAll();

    /**
     * Récupère tous les managers avec pagination
     */
    Page<ManagerDTO> findAll(Pageable pageable);

    /**
     * Récupère un manager par son ID
     */
    Optional<ManagerDTO> findById(Long id);

    /**
     * Récupère un manager par son employé
     */
    Optional<ManagerDTO> findByEmployeId(Long employeId);

    /**
     * Récupère un manager par matricule
     */
    Optional<ManagerDTO> findByEmployeMatricule(String matricule);

    /**
     * Récupère les managers par département
     */
    List<ManagerDTO> findByDepartement(String departement);

    /**
     * Récupère les managers actifs
     */
    List<ManagerDTO> findManagersActifs();

    /**
     * Récupère les managers sans équipe
     */
    List<ManagerDTO> findManagersSansEquipe();

    /**
     * Récupère les managers avec demandes en attente
     */
    List<ManagerDTO> findManagersAvecDemandesEnAttente();

    /**
     * Récupère les managers avec demandes urgentes
     */
    List<ManagerDTO> findManagersAvecDemandesUrgentes();

    /**
     * Crée un nouveau manager
     */
    ManagerDTO create(ManagerDTO dto);

    /**
     * Met à jour un manager
     */
    ManagerDTO update(Long id, ManagerDTO dto);

    /**
     * Active un manager
     */
    ManagerDTO activer(Long id);

    /**
     * Désactive un manager
     */
    ManagerDTO desactiver(Long id);

    /**
     * Ajoute un employé à l'équipe du manager
     */
    ManagerDTO ajouterEmploye(Long managerId, Long employeId);

    /**
     * Retire un employé de l'équipe du manager
     */
    ManagerDTO retirerEmploye(Long managerId, Long employeId);

    /**
     * Récupère les statistiques des managers
     */
    Map<String, Object> getManagersStats();

    /**
     * Compte les managers par département
     */
    Map<String, Long> countByDepartement();

    /**
     * Calcule l'ancienneté moyenne des managers
     */
    Double calculerAncienneteMoyenne();

    /**
     * Recherche des managers par mot-clé
     */
    List<ManagerDTO> search(String keyword);

    /**
     * Récupère les managers récents
     */
    List<ManagerDTO> findManagersRecents(int limit);

    /**
     * Génère un rapport d'équipe pour un manager
     */
    String genererRapportEquipe(Long managerId);

    /**
     * Récupère les statistiques pour le tableau de bord
     */
    List<Object[]> getStatsManagers();

    default void delete(Long id) {
        // Par défaut, on fait un soft delete
        desactiver(id);
    }
}