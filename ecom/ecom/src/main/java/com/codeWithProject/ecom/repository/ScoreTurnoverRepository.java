package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.ScoreTurnover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScoreTurnoverRepository extends JpaRepository<ScoreTurnover, Long> {

    List<ScoreTurnover> findByEmployeId(Long employeId);

    void deleteByEmployeId(Long employeId);

    @Query("""
        SELECT s
        FROM ScoreTurnover s
        WHERE s.employe.id = :employeId
        ORDER BY s.datePrediction DESC, s.id DESC
    """)
    List<ScoreTurnover> findHistoriqueEmploye(@Param("employeId") Long employeId);

    @Query("""
        SELECT s
        FROM ScoreTurnover s
        WHERE s.employe.id = :employeId
        ORDER BY s.datePrediction DESC, s.id DESC
    """)
    List<ScoreTurnover> findDernierScoreEmploye(@Param("employeId") Long employeId);

    Optional<ScoreTurnover> findTopByEmployeIdOrderByDatePredictionDescIdDesc(Long employeId);

    Optional<ScoreTurnover> findByEmployeIdAndDatePrediction(Long employeId, LocalDate datePrediction);

    List<ScoreTurnover> findByNiveauRisque(String niveauRisque);

    @Query("""
        SELECT s
        FROM ScoreTurnover s
        WHERE s.niveauRisque IN ('ELEVE', 'CRITIQUE')
        ORDER BY s.score DESC
    """)
    List<ScoreTurnover> findScoresRisques();

    @Query("""
        SELECT s
        FROM ScoreTurnover s
        WHERE s.niveauRisque = 'CRITIQUE'
        ORDER BY s.score DESC
    """)
    List<ScoreTurnover> findScoresCritiques();

    List<ScoreTurnover> findByScoreGreaterThan(Double seuil);

    List<ScoreTurnover> findByScoreLessThan(Double seuil);

    List<ScoreTurnover> findByDatePredictionAfter(LocalDate date);

    List<ScoreTurnover> findByDatePredictionBefore(LocalDate date);

    List<ScoreTurnover> findByDatePredictionBetween(LocalDate debut, LocalDate fin);

@Query("""
    SELECT s
    FROM ScoreTurnover s
    ORDER BY s.id DESC
""")
List<ScoreTurnover> findDerniersScores(Pageable pageable);

    @Query("""
        SELECT s
        FROM ScoreTurnover s
        WHERE s.datePrediction < :dateLimite
    """)
    List<ScoreTurnover> findScoresObsoletes(@Param("dateLimite") LocalDate dateLimite);

    @Query("""
        SELECT s.niveauRisque, COUNT(s)
        FROM ScoreTurnover s
        WHERE s.datePrediction = (
            SELECT MAX(s2.datePrediction)
            FROM ScoreTurnover s2
            WHERE s2.employe.id = s.employe.id
        )
        AND s.id = (
            SELECT MAX(s3.id)
            FROM ScoreTurnover s3
            WHERE s3.employe.id = s.employe.id
            AND s3.datePrediction = s.datePrediction
        )
        GROUP BY s.niveauRisque
    """)
    List<Object[]> repartitionRisquesActuels();

    @Query("""
        SELECT AVG(s.score)
        FROM ScoreTurnover s
        WHERE s.datePrediction = (
            SELECT MAX(s2.datePrediction)
            FROM ScoreTurnover s2
            WHERE s2.employe.id = s.employe.id
        )
        AND s.id = (
            SELECT MAX(s3.id)
            FROM ScoreTurnover s3
            WHERE s3.employe.id = s.employe.id
            AND s3.datePrediction = s.datePrediction
        )
    """)
    Double scoreMoyenActuel();

    @Query("""
        SELECT s.employe.departement, AVG(s.score)
        FROM ScoreTurnover s
        WHERE s.datePrediction = (
            SELECT MAX(s2.datePrediction)
            FROM ScoreTurnover s2
            WHERE s2.employe.id = s.employe.id
        )
        AND s.id = (
            SELECT MAX(s3.id)
            FROM ScoreTurnover s3
            WHERE s3.employe.id = s.employe.id
            AND s3.datePrediction = s.datePrediction
        )
        GROUP BY s.employe.departement
    """)
    List<Object[]> scoreMoyenParDepartement();

    @Query("""
        SELECT s
        FROM ScoreTurnover s
        WHERE s.actionRecommandee IS NOT NULL
        AND s.actionRecommandee <> ''
    """)
    List<ScoreTurnover> findAvecActionsRecommandees();

    @Query("""
        SELECT COUNT(s) > 0
        FROM ScoreTurnover s
        WHERE s.employe.id = :employeId
        AND s.datePrediction >= :dateLimite
    """)
    boolean hasScoreRecent(
            @Param("employeId") Long employeId,
            @Param("dateLimite") LocalDate dateLimite
    );

    @Query("""
        SELECT
            COUNT(s),
            AVG(s.score),
            MIN(s.score),
            MAX(s.score),
            SUM(CASE WHEN s.niveauRisque = 'CRITIQUE' THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.niveauRisque = 'ELEVE' THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.niveauRisque = 'MOYEN' THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.niveauRisque = 'FAIBLE' THEN 1 ELSE 0 END),
            AVG(s.confianceModele)
        FROM ScoreTurnover s
        WHERE s.datePrediction = (
            SELECT MAX(s2.datePrediction)
            FROM ScoreTurnover s2
            WHERE s2.employe.id = s.employe.id
        )
        AND s.id = (
            SELECT MAX(s3.id)
            FROM ScoreTurnover s3
            WHERE s3.employe.id = s.employe.id
            AND s3.datePrediction = s.datePrediction
        )
    """)
    List<Object[]> getStatsTableauBord();
}