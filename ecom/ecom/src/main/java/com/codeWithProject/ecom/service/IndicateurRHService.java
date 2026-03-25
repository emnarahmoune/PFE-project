package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des indicateurs RH
 */
public interface IndicateurRHService {

    /**
     * Récupère tous les indicateurs
     */
    List<IndicateurRHDTO> findAll();

    /**
     * Récupère tous les indicateurs avec pagination
     */
    Page<IndicateurRHDTO> findAll(Pageable pageable);

    /**
     * Récupère un indicateur par son ID
     */
    Optional<IndicateurRHDTO> findById(Long id);

    /**
     * Récupère les indicateurs par type
     */
    List<IndicateurRHDTO> findByType(String type);

    /**
     * Récupère les indicateurs par période
     */
    List<IndicateurRHDTO> findByPeriode(String periode);

    /**
     * Récupère les indicateurs par département
     */
    List<IndicateurRHDTO> findByDepartement(String departement);

    /**
     * Récupère le dernier indicateur d'un type
     */
    Optional<IndicateurRHDTO> findDernierIndicateurByType(String type);

    /**
     * Récupère les indicateurs récents
     */
    List<IndicateurRHDTO> findIndicateursRecents(int jours);

    /**
     * Récupère les indicateurs avec alerte
     */
    List<IndicateurRHDTO> findAvecAlerte();

    /**
     * Crée un nouvel indicateur
     */
    IndicateurRHDTO create(IndicateurRHDTO dto);

    /**
     * Calcule un indicateur de turnover
     */
    IndicateurRHDTO calculerTurnover(LocalDate dateDebut, LocalDate dateFin, String periode, String departement);

    /**
     * Calcule un indicateur d'absentéisme
     */
    IndicateurRHDTO calculerAbsenteisme(LocalDate dateDebut, LocalDate dateFin, String periode, String departement);

    /**
     * Calcule un indicateur de performance
     */
    IndicateurRHDTO calculerPerformance(LocalDate dateCalcul, String periode, String departement);

    /**
     * Calcule un indicateur de satisfaction
     */
    IndicateurRHDTO calculerSatisfaction(LocalDate dateCalcul, String periode, String departement);

    /**
     * Calcule un indicateur de couverture des compétences
     */
    IndicateurRHDTO calculerCouvertureCompetences(LocalDate dateCalcul, String periode, String departement);

    /**
     * Met à jour un indicateur
     */
    IndicateurRHDTO update(Long id, IndicateurRHDTO dto);

    /**
     * Supprime un indicateur
     */
    void delete(Long id);

    /**
     * Récupère les statistiques par type
     */
    Map<String, Double> getMoyennesByType();

    /**
     * Récupère l'historique d'un indicateur
     */
    List<IndicateurRHDTO> getHistoriqueIndicateur(String type, int limite);

    /**
     * Récupère les indicateurs en hausse
     */
    List<IndicateurRHDTO> findEnHausse(String type);

    /**
     * Récupère les indicateurs en baisse
     */
    List<IndicateurRHDTO> findEnBaisse(String type);

    /**
     * Récupère les derniers indicateurs pour le tableau de bord
     */
    Map<String, IndicateurRHDTO> getDerniersIndicateurs();

    /**
     * Récupère les statistiques globales
     */
    Map<String, Object> getStatsTableauBord();
}