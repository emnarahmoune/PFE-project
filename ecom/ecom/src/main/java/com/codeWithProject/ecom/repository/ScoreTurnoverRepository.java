package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.ScoreTurnover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScoreTurnoverRepository extends JpaRepository<ScoreTurnover, Long> {

    // ===== RECHERCHES PAR EMPLOYÉ =====
    List<ScoreTurnover> findByEmployeId(Long employeId);

    @Query("SELECT s FROM ScoreTurnover s WHERE s.employe.id = :employeId ORDER BY s.datePrediction DESC")
    List<ScoreTurnover> findHistoriqueEmploye(@Param("employeId") Long employeId);

    @Query("SELECT s FROM ScoreTurnover s WHERE s.employe.id = :employeId ORDER BY s.datePrediction DESC")
    List<ScoreTurnover> findDernierScoreEmploye(@Param("employeId") Long employeId);

    // ===== RECHERCHES PAR NIVEAU DE RISQUE =====
    List<ScoreTurnover> findByNiveauRisque(String niveauRisque);

    @Query("SELECT s FROM ScoreTurnover s WHERE s.niveauRisque IN ('ELEVE', 'CRITIQUE') ORDER BY s.score DESC")
    List<ScoreTurnover> findScoresRisques();

    @Query("SELECT s FROM ScoreTurnover s WHERE s.niveauRisque = 'CRITIQUE' ORDER BY s.score DESC")
    List<ScoreTurnover> findScoresCritiques();

    // ===== RECHERCHES PAR SCORE =====
    List<ScoreTurnover> findByScoreGreaterThan(Double seuil);
    List<ScoreTurnover> findByScoreLessThan(Double seuil);

    // ===== RECHERCHES PAR DATE =====
    List<ScoreTurnover> findByDatePredictionAfter(LocalDate date);
    List<ScoreTurnover> findByDatePredictionBefore(LocalDate date);
    List<ScoreTurnover> findByDatePredictionBetween(LocalDate debut, LocalDate fin);

    @Query("SELECT s FROM ScoreTurnover s WHERE s.datePrediction = (SELECT MAX(s2.datePrediction) FROM ScoreTurnover s2)")
    List<ScoreTurnover> findDerniersScores();

    @Query("SELECT s FROM ScoreTurnover s WHERE s.datePrediction < :dateLimite")
    List<ScoreTurnover> findScoresObsoletes(@Param("dateLimite") LocalDate dateLimite);

    // ===== STATISTIQUES =====
    @Query("SELECT s.niveauRisque, COUNT(s) FROM ScoreTurnover s WHERE s.datePrediction = (SELECT MAX(s2.datePrediction) FROM ScoreTurnover s2) GROUP BY s.niveauRisque")
    List<Object[]> repartitionRisquesActuels();

    @Query("SELECT AVG(s.score) FROM ScoreTurnover s WHERE s.datePrediction = (SELECT MAX(s2.datePrediction) FROM ScoreTurnover s2)")
    Double scoreMoyenActuel();

    @Query("SELECT s.employe.departement, AVG(s.score) FROM ScoreTurnover s GROUP BY s.employe.departement")
    List<Object[]> scoreMoyenParDepartement();

    // ===== RECHERCHES SPÉCIFIQUES =====
    @Query("SELECT s FROM ScoreTurnover s WHERE s.actionRecommandee IS NOT NULL AND s.actionRecommandee != ''")
    List<ScoreTurnover> findAvecActionsRecommandees();

    @Query("SELECT COUNT(s) > 0 FROM ScoreTurnover s WHERE s.employe.id = :employeId AND s.datePrediction >= :dateLimite")
    boolean hasScoreRecent(@Param("employeId") Long employeId, @Param("dateLimite") LocalDate dateLimite);

    // ===== TABLEAU DE BORD =====
    @Query("SELECT new map(" +
            "COUNT(s) as totalScores, " +
            "AVG(s.score) as scoreMoyen, " +
            "MIN(s.score) as scoreMin, " +
            "MAX(s.score) as scoreMax, " +
            "SUM(CASE WHEN s.niveauRisque = 'CRITIQUE' THEN 1 ELSE 0 END) as risqueCritique, " +
            "SUM(CASE WHEN s.niveauRisque = 'ELEVE' THEN 1 ELSE 0 END) as risqueEleve, " +
            "SUM(CASE WHEN s.niveauRisque = 'MOYEN' THEN 1 ELSE 0 END) as risqueMoyen, " +
            "SUM(CASE WHEN s.niveauRisque = 'FAIBLE' THEN 1 ELSE 0 END) as risqueFaible, " +
            "AVG(s.confianceModele) as confianceMoyenne) " +
            "FROM ScoreTurnover s WHERE s.datePrediction = (SELECT MAX(s2.datePrediction) FROM ScoreTurnover s2)")
    List<Object[]> getStatsTableauBord();
}