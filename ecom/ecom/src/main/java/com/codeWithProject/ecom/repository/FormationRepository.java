package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.Formation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {

    // =========================
    // 🔍 FILTRES
    // =========================

    List<Formation> findByDomaine(String domaine);

    List<Formation> findByActifTrue();

    List<Formation> findByTitreContainingIgnoreCase(String titre);

    // =========================
    // 👤 FORMATIONS PAR EMPLOYÉ
    // =========================

    @Query("""
        SELECT f
        FROM Formation f
        JOIN f.employes e
        WHERE e.id = :employeId
    """)
    List<Formation> findFormationsByEmployeId(@Param("employeId") Long employeId);

    // =========================
    // 🚫 NON SUIVIES
    // =========================

    @Query("""
        SELECT f FROM Formation f
        WHERE f.id NOT IN (
            SELECT ef.formation.id
            FROM EmployeFormation ef
            WHERE ef.employe.id = :employeId
        )
    """)
    List<Formation> findFormationsNonSuivies(@Param("employeId") Long employeId);

    // =========================
    // 🤖 RECOMMANDATIONS
    // =========================

    @Query("""
        SELECT f FROM Formation f
        WHERE f.domaine IN :domaines
        AND f.id NOT IN (
            SELECT ef.formation.id
            FROM EmployeFormation ef
            WHERE ef.employe.id = :employeId
        )
    """)
    List<Formation> findFormationsRecommandees(
            @Param("domaines") List<String> domaines,
            @Param("employeId") Long employeId
    );

    // =========================
    // 📊 FORMATIONS + EMPLOYE
    // =========================

    @Query("""
        SELECT ef FROM EmployeFormation ef
        JOIN FETCH ef.formation
        WHERE ef.employe.id = :employeId
    """)
    List<EmployeFormation> findEmployeFormationsWithFormation(@Param("employeId") Long employeId);

    // =========================
    // 🔥 DÉTAIL FORMATION (FIX LAZY)
    // =========================

    @Query("""
        SELECT DISTINCT f FROM Formation f
        LEFT JOIN FETCH f.videos
        LEFT JOIN FETCH f.supports
        WHERE f.id = :id
    """)
    Formation findByIdWithDetails(@Param("id") Long id);

    // =========================
    // 🔥 LISTE FORMATIONS (ANTI BUG)
    // =========================

   @Query("""
SELECT DISTINCT f FROM Formation f
LEFT JOIN FETCH f.videos
""")
List<Formation> findAllWithDetails();


@Query(value = """
    SELECT DISTINCT f.*
    FROM formations f
    INNER JOIN formation_competence fc ON fc.formation_id = f.id
    WHERE fc.competence_id IN (:competenceIds)
      AND (f.actif = true OR f.actif IS NULL)
      AND NOT EXISTS (
          SELECT 1
          FROM employe_formation ef
          WHERE ef.formation_id = f.id
            AND ef.employe_id = :employeId
      )
    LIMIT 3
""", nativeQuery = true)
List<Formation> findRecommendedByCompetenceIds(
        @Param("employeId") Long employeId,
        @Param("competenceIds") List<Long> competenceIds
);
}