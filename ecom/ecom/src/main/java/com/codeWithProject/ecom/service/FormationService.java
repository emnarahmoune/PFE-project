package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.FormationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des formations
 */
public interface FormationService {

    /**
     * Récupère toutes les formations
     */
    List<FormationDTO> findAll();

    /**
     * Récupère toutes les formations avec pagination
     */
    Page<FormationDTO> findAll(Pageable pageable);

    /**
     * Récupère une formation par son ID
     */
    Optional<FormationDTO> findById(Long id);

    /**
     * Récupère une formation par son titre
     */
    Optional<FormationDTO> findByTitre(String titre);

    /**
     * Récupère les formations par domaine
     */
    List<FormationDTO> findByDomaine(String domaine);

    /**
     * Récupère les formations actives
     */
    List<FormationDTO> findActives();

    /**
     * Récupère les formations récentes
     */
    List<FormationDTO> findFormationsRecentes();

    /**
     * Crée une nouvelle formation
     */
    FormationDTO create(FormationDTO dto);

    /**
     * Met à jour une formation
     */
    FormationDTO update(Long id, FormationDTO dto);

    /**
     * Supprime une formation (désactive)
     */
    void delete(Long id);

    /**
     * Active une formation
     */
    FormationDTO activer(Long id);

    /**
     * Désactive une formation
     */
    FormationDTO desactiver(Long id);

    /**
     * Ajoute un participant à une formation
     */
    FormationDTO ajouterParticipant(Long formationId, Long employeId);

    /**
     * Retire un participant d'une formation
     */
    FormationDTO retirerParticipant(Long formationId, Long employeId);

    /**
     * Récupère les formations d'un employé
     */
    List<FormationDTO> findFormationsByEmployeId(Long employeId);

    /**
     * Récupère les formations non suivies par un employé
     */
    List<FormationDTO> findFormationsNonSuiviesParEmploye(Long employeId);

    /**
     * Récupère les formations les plus suivies
     */
    List<FormationDTO> findFormationsPopulaires(int limit);

    /**
     * Compte le nombre de formations par domaine
     */
    Map<String, Long> countByDomaine();

    /**
     * Calcule la durée moyenne des formations
     */
    Double calculerDureeMoyenne();

    /**
     * Recherche des formations par mot-clé
     */
    List<FormationDTO> search(String keyword);

    /**
     * Récupère tous les domaines distincts
     */
    List<String> findAllDomaines();

    /**
     * Récupère les statistiques globales
     */
    Map<String, Object> getStatsTableauBord();
}