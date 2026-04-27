package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {

    // 🔍 rechercher par domaine
    List<Formation> findByDomaine(String domaine);

    // 🔍 formations actives
    List<Formation> findByActifTrue();




    @Query("""
    SELECT f FROM Formation f
    WHERE f.id NOT IN (
        SELECT ef.formation.id
        FROM EmployeFormation ef
        WHERE ef.employe.id = :employeId
    )
""")
List<Formation> findFormationsNonSuiviesParEmploye(@Param("employeId") Long employeId);

    // 🔍 recherche par titre
    List<Formation> findByTitreContainingIgnoreCase(String titre);

@Query("""
SELECT f FROM Formation f
WHERE f.domaine IN :domaines
AND f.id NOT IN (
    SELECT ef.formation.id FROM EmployeFormation ef
    WHERE ef.employe.id = :employeId
)
""")
List<Formation> findFormationsRecommandees(
        @Param("domaines") List<String> domaines,
        @Param("employeId") Long employeId
);

@Query("""
SELECT ef FROM EmployeFormation ef
JOIN FETCH ef.formation
WHERE ef.employe.id = :employeId
""")
List<EmployeFormation> findByEmployeId(@Param("employeId") Long employeId);


@Query("""
SELECT ef FROM EmployeFormation ef
JOIN FETCH ef.formation
WHERE ef.employe.id = :employeId
""")
List<EmployeFormation> findByEmployeIdWithFormation(Long employeId);



@Query("""
SELECT f FROM Formation f
WHERE f.id NOT IN (
    SELECT ef.formation.id FROM EmployeFormation ef
    WHERE ef.employe.id = :employeId
)
""")
List<Formation> findFormationsNonSuivies(@Param("employeId") Long employeId);
}