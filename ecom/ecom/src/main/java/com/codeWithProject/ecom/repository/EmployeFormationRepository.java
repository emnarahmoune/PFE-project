package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.Formation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;


@Repository
public interface EmployeFormationRepository extends JpaRepository<EmployeFormation, Long> {

    //  formations d’un employé
List<EmployeFormation> findByEmploye_Id(Long employeId);
    //  employés inscrits à une formation
    List<EmployeFormation> findByFormation_Id(Long formationId);

    //  vérifier si déjà inscrit
    boolean existsByEmploye_IdAndFormation_Id(Long employeId, Long formationId);

    //  récupérer une inscription spécifique
    Optional<EmployeFormation> findByEmploye_IdAndFormation_Id(Long employeId, Long formationId);

    //  formations en cours
    List<EmployeFormation> findByEmploye_IdAndStatut(Long employeId, String statut);

    //  formations terminées
    List<EmployeFormation> findByStatut(String statut);

boolean existsByEmployeIdAndFormationId(Long employeId, Long formationId);

    //  formations populaires
    @Query("SELECT ef.formation.id, COUNT(ef) FROM EmployeFormation ef GROUP BY ef.formation.id ORDER BY COUNT(ef) DESC")
    List<Object[]> findFormationsPopulaires();

    //  progression avancée (> 70%)
    List<EmployeFormation> findByProgressionGreaterThan(Integer progression);


    Optional<EmployeFormation> findByEmployeIdAndFormationId(Long empId, Long formId);
@Query("""
SELECT ef FROM EmployeFormation ef
JOIN FETCH ef.formation
WHERE ef.employe.id = :employeId
""")
List<EmployeFormation> findByEmployeId(@Param("employeId") Long employeId);

}