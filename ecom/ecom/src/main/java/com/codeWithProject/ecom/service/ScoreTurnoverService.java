package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.EmployeScoreDetailDTO;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des scores de turnover
 */
public interface ScoreTurnoverService {

    /**
     * Récupère tous les scores
     */
    List<ScoreTurnoverDTO> findAll();

    /**
     * Récupère tous les scores avec pagination
     */
    Page<ScoreTurnoverDTO> findAll(Pageable pageable);

    /**
     * Récupère un score par son ID
     */
    Optional<ScoreTurnoverDTO> findById(Long id);

    /**
     * Récupère les scores d'un employé
     */
    List<ScoreTurnoverDTO> findByEmployeId(Long employeId);

    /**
     * Récupère l'historique des scores d'un employé
     */
    List<ScoreTurnoverDTO> findHistoriqueEmploye(Long employeId);

    /**
     * Récupère le dernier score d'un employé
     */
    Optional<ScoreTurnoverDTO> findDernierScoreEmploye(Long employeId);

    /**
     * Récupère les scores par niveau de risque
     */
    List<ScoreTurnoverDTO> findByNiveauRisque(String niveauRisque);

    /**
     * Récupère les scores à risque (ELEVE ou CRITIQUE)
     */
    List<ScoreTurnoverDTO> findScoresRisques();

    /**
     * Récupère les scores critiques
     */
    List<ScoreTurnoverDTO> findScoresCritiques();

    /**
     * Récupère les derniers scores de tous les employés
     */
    List<ScoreTurnoverDTO> findDerniersScores();

    /**
     * Calcule un nouveau score pour un employé
     */
    ScoreTurnoverDTO calculerScorePourEmploye(Long employeId, Long systemeBIId);

    /**
     * Calcule les scores pour tous les employés
     */
    List<ScoreTurnoverDTO> calculerScoresPourTousEmployes(Long systemeBIId);

    /**
     * Prédit le risque de départ pour un employé
     */
    ScoreTurnoverDTO predireRisque(Long employeId, Long systemeBIId);

    /**
     * Supprime un score
     */
    void delete(Long id);

    /**
     * Supprime les scores obsolètes
     */
    void deleteScoresObsoletes(int jours);

    /**
     * Récupère la répartition des risques actuels
     */
    Map<String, Long> getRepartitionRisques();

    /**
     * Calcule le score moyen actuel
     */
    Double getScoreMoyenActuel();

    /**
     * Calcule le score moyen par département
     */
    Map<String, Double> getScoreMoyenParDepartement();

    /**
     * Récupère les scores avec actions recommandées
     */
    List<ScoreTurnoverDTO> findScoresAvecActions();

    /**
     * Récupère les statistiques pour le tableau de bord
     */
    Map<String, Object> getStatsTableauBord();

    /**
     * Vérifie si un employé a un score récent
     */
    boolean hasScoreRecent(Long employeId, int jours);

    EmployeScoreDetailDTO getEmployeScoreDetail(Long employeId);
}