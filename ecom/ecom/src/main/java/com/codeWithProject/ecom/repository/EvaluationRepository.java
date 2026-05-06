package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // =========================
    // EMPLOYÉ
    // =========================

    List<Evaluation> findByEmploye_IdOrderByDateEvaluationDesc(Long employeId);

    Optional<Evaluation> findTopByEmploye_IdOrderByDateEvaluationDesc(Long employeId);

    List<Evaluation> findByEmploye_IdAndDateEvaluationBetween(
            Long employeId,
            LocalDate debut,
            LocalDate fin
    );

    // =========================
    // MANAGER
    // =========================

    /**
     * Évaluations créées par un manager.
     */
    List<Evaluation> findByEvaluateur_IdOrderByDateEvaluationDesc(Long evaluateurId);

    /**
     * Évaluations des employés appartenant à l'équipe du manager.
     */
    List<Evaluation> findByEmploye_Manager_IdOrderByDateEvaluationDesc(Long managerId);

    // =========================
    // STATS
    // =========================

    @Query("""
           SELECT AVG(e.note)
           FROM Evaluation e
           WHERE e.employe.id = :employeId
             AND e.dateEvaluation BETWEEN :debut AND :fin
           """)
    Double moyenneEvaluationSurPeriode(
            @Param("employeId") Long employeId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin
    );

    @Query("""
           SELECT AVG(e.note)
           FROM Evaluation e
           WHERE e.employe.id = :employeId
           """)
    Double moyenneGlobaleEmploye(@Param("employeId") Long employeId);

    @Query("""
           SELECT AVG(e.note)
           FROM Evaluation e
           WHERE e.employe.manager.id = :managerId
           """)
    Double moyenneEquipeManager(@Param("managerId") Long managerId);

    @Query("""
           SELECT AVG(e.note)
           FROM Evaluation e
           """)
    Double moyenneGlobale();

    @Query("""
           SELECT AVG(e.objectifsAtteints)
           FROM Evaluation e
           WHERE e.objectifsAtteints IS NOT NULL
           """)
    Double moyenneObjectifsGlobale();

    @Query("""
           SELECT AVG(e.objectifsAtteints)
           FROM Evaluation e
           WHERE e.employe.manager.id = :managerId
             AND e.objectifsAtteints IS NOT NULL
           """)
    Double moyenneObjectifsManager(@Param("managerId") Long managerId);

    @Query("""
           SELECT AVG(e.objectifsAtteints)
           FROM Evaluation e
           WHERE e.employe.id = :employeId
             AND e.objectifsAtteints IS NOT NULL
           """)
    Double moyenneObjectifsEmploye(@Param("employeId") Long employeId);

    @Query("""
           SELECT COUNT(e)
           FROM Evaluation e
           WHERE e.note >= :noteMin
           """)
    Long countByNoteGreaterThanOrEqual(@Param("noteMin") Double noteMin);

    @Query("""
           SELECT COUNT(e)
           FROM Evaluation e
           WHERE e.note < :noteMax
           """)
    Long countByNoteLessThan(@Param("noteMax") Double noteMax);

    @Query("""
           SELECT COUNT(e)
           FROM Evaluation e
           WHERE e.employe.manager.id = :managerId
             AND e.note >= :noteMin
           """)
    Long countManagerByNoteGreaterThanOrEqual(
            @Param("managerId") Long managerId,
            @Param("noteMin") Double noteMin
    );

    @Query("""
           SELECT COUNT(e)
           FROM Evaluation e
           WHERE e.employe.manager.id = :managerId
             AND e.note < :noteMax
           """)
    Long countManagerByNoteLessThan(
            @Param("managerId") Long managerId,
            @Param("noteMax") Double noteMax
    );
}