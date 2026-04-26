package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // Récupérer toutes les évaluations d'un employé, triées par date décroissante
    List<Evaluation> findByEmployeIdOrderByDateEvaluationDesc(Long employeId);

    // Dernière évaluation d'un employé
    Optional<Evaluation> findTopByEmployeIdOrderByDateEvaluationDesc(Long employeId);

    // Évaluations sur une période
    List<Evaluation> findByEmployeIdAndDateEvaluationBetween(Long employeId, LocalDate debut, LocalDate fin);

    // Moyenne des notes sur une année donnée (utilisée dans le calcul du score turnover)
    @Query("SELECT AVG(e.note) FROM Evaluation e WHERE e.employe.id = :employeId AND YEAR(e.dateEvaluation) = :annee")
    Double moyenneEvaluationAnnuelle(@Param("employeId") Long employeId, @Param("annee") int annee);

    // Évaluations pour un manager (les employés dont il est le manager)
    @Query("SELECT e FROM Evaluation e WHERE e.employe.manager.id = :managerId ORDER BY e.dateEvaluation DESC")
    List<Evaluation> findByManagerId(@Param("managerId") Long managerId);
}