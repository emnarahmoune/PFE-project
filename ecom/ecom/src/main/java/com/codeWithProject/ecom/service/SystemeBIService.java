package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.SystemeBIDTO;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion du système BI
 */
public interface SystemeBIService {

    /**
     * Récupère tous les systèmes BI
     */
    List<SystemeBIDTO> findAll();

    /**
     * Récupère tous les systèmes BI avec pagination
     */
    Page<SystemeBIDTO> findAll(Pageable pageable);

    /**
     * Récupère un système BI par son ID
     */
    Optional<SystemeBIDTO> findById(Long id);

    /**
     * Récupère un système BI par sa version
     */
    Optional<SystemeBIDTO> findByVersion(String version);

    /**
     * Récupère les systèmes BI actifs
     */
    List<SystemeBIDTO> findSystemesActifs();

    /**
     * Récupère les systèmes BI en maintenance
     */
    List<SystemeBIDTO> findSystemesEnMaintenance();

    /**
     * Crée un nouveau système BI
     */
    SystemeBIDTO create(SystemeBIDTO dto);

    /**
     * Met à jour un système BI
     */
    SystemeBIDTO update(Long id, SystemeBIDTO dto);

    /**
     * Supprime un système BI
     */
    void delete(Long id);

    /**
     * Active un système BI
     */
    SystemeBIDTO activer(Long id);

    /**
     * Met en maintenance un système BI
     */
    SystemeBIDTO mettreEnMaintenance(Long id);

    /**
     * Désactive un système BI
     */
    SystemeBIDTO desactiver(Long id);

    /**
     * Exécute l'ETL
     */
    SystemeBIDTO executerETL(Long id);

    /**
     * Analyse les compétences
     */
    Map<String, Object> analyserCompetences(Long id);

    /**
     * Recommande des formations pour un employé
     */
    List<FormationDTO> recommanderFormations(Long id, Long employeId);

    /**
     * Analyse le turnover
     */
    IndicateurRHDTO analyserTurnover(Long id, String periode, String departement);

    /**
     * Analyse l'absentéisme
     */
    IndicateurRHDTO analyserAbsenteisme(Long id, String periode, String departement);

    /**
     * Prédit le turnover pour tous les employés
     */
    List<ScoreTurnoverDTO> predireTurnover(Long id);

    /**
     * Prédit le turnover pour un employé
     */
    ScoreTurnoverDTO predireTurnoverEmploye(Long id, Long employeId);

    /**
     * Récupère les statistiques par statut
     */
    Map<String, Long> countByStatut();

    /**
     * Récupère la dernière exécution globale
     */
    LocalDateTime getDerniereExecutionGlobale();

    /**
     * Recherche des systèmes BI par mot-clé
     */
    List<SystemeBIDTO> search(String keyword);

    /**
     * Récupère les statistiques pour le tableau de bord
     */
    Map<String, Object> getStatsTableauBord();
}