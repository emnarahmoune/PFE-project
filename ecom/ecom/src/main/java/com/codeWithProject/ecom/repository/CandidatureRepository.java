package com.codeWithProject.ecom.repository;


import com.codeWithProject.ecom.entity.Candidature;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.OffreRecrutement;
import com.codeWithProject.ecom.entity.enums.StatutCandidature;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    boolean existsByOffreAndEmploye(OffreRecrutement offre, Employe employe);

    boolean existsByOffreIdAndEmployeId(Long offreId, Long employeId);

    List<Candidature> findByEmployeIdOrderByDateSoumissionDesc(Long employeId);

    List<Candidature> findByOffreIdOrderByDateSoumissionDesc(Long offreId);

    List<Candidature> findByStatutOrderByDateSoumissionDesc(StatutCandidature statut);

    @EntityGraph(attributePaths = {
            "offre",
            "employe",
            "score",
            "analyseCv"
    })
    List<Candidature> findWithOffreAndEmployeAndScoreByOffreIdOrderByDateSoumissionDesc(Long offreId);

    @EntityGraph(attributePaths = {
            "offre",
            "employe",
            "score",
            "analyseCv"
    })
    List<Candidature> findWithOffreAndEmployeAndScoreByEmployeIdOrderByDateSoumissionDesc(Long employeId);

    @Query("""
        SELECT c
        FROM Candidature c
        LEFT JOIN FETCH c.offre
        LEFT JOIN FETCH c.employe
        LEFT JOIN FETCH c.score s
        WHERE c.offre.id = :offreId
        AND s IS NOT NULL
        ORDER BY s.scoreGlobal DESC
    """)
    List<Candidature> findTopCandidaturesByOffreId(Long offreId, Pageable pageable);
}