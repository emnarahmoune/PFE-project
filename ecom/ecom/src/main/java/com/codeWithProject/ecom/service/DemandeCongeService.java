package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des demandes de congé
 */
public interface DemandeCongeService {

    /**
     * Récupère toutes les demandes de congé
     */
    List<DemandeCongeDTO> findAll();

    /**
     * Récupère toutes les demandes avec pagination
     */
    Page<DemandeCongeDTO> findAll(Pageable pageable);

    /**
     * Récupère une demande par son ID
     */
    Optional<DemandeCongeDTO> findById(Long id);

    /**
     * Récupère les demandes d'un employé
     */
    List<DemandeCongeDTO> findByEmployeId(Long employeId);

    /**
     * Récupère les demandes d'un manager
     */
    List<DemandeCongeDTO> findByManagerId(Long managerId);

    /**
     * Récupère les demandes par statut
     */
    List<DemandeCongeDTO> findByStatut(String statut);

    /**
     * Récupère les demandes par type
     */
    List<DemandeCongeDTO> findByType(String type);

    /**
     * Récupère les demandes urgentes
     */
    List<DemandeCongeDTO> findDemandesUrgentes();

    /**
     * Récupère les demandes en attente pour un manager
     */
    List<DemandeCongeDTO> findDemandesEnAttentePourManager(Long managerId);

    /**
     * Récupère les congés en cours
     */
    List<DemandeCongeDTO> findCongesEnCours();

    /**
     * Crée une nouvelle demande de congé
     */
    DemandeCongeDTO create(DemandeCongeDTO dto);

    /**
     * Modifie une demande de congé (employé)
     */
    DemandeCongeDTO modifier(Long id, DemandeCongeDTO dto);

    /**
     * Annule une demande de congé (employé)
     */
    DemandeCongeDTO annuler(Long id);

    /**
     * Valide une demande de congé (manager)
     */
    DemandeCongeDTO valider(Long id, Long managerId);

    /**
     * Refuse une demande de congé (manager)
     */
    DemandeCongeDTO refuser(Long id, Long managerId, String motif);

    /**
     * Supprime une demande (admin seulement)
     */
    void delete(Long id);

    /**
     * Compte le nombre de demandes par statut
     */
    Map<String, Long> countByStatut();

    /**
     * Compte le nombre de demandes par type
     */
    Map<String, Long> countByType();

    /**
     * Statistiques mensuelles des demandes
     */
    Map<Integer, Long> getStatsMensuelles(int annee);

    /**
     * Vérifie si un employé a des demandes en conflit
     */
    boolean hasConflitDates(Long employeId, LocalDate debut, LocalDate fin, Long demandeId);
}