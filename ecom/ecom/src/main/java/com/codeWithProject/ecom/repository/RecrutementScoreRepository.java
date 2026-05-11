package com.codeWithProject.ecom.repository;


import com.codeWithProject.ecom.entity.RecrutementScore;
import com.codeWithProject.ecom.entity.enums.NiveauCompatibilite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecrutementScoreRepository extends JpaRepository<RecrutementScore, Long> {

    Optional<RecrutementScore> findByCandidatureId(Long candidatureId);

    Optional<RecrutementScore> findByOffreIdAndEmployeId(Long offreId, Long employeId);

    boolean existsByCandidatureId(Long candidatureId);

    void deleteByCandidatureId(Long candidatureId);

    List<RecrutementScore> findByOffreIdOrderByScoreGlobalDesc(Long offreId);

    List<RecrutementScore> findByOffreIdOrderByScoreGlobalDesc(Long offreId, Pageable pageable);

    List<RecrutementScore> findByNiveauCompatibiliteOrderByScoreGlobalDesc(NiveauCompatibilite niveauCompatibilite);

    @EntityGraph(attributePaths = {
            "candidature",
            "offre",
            "employe"
    })
    List<RecrutementScore> findWithCandidatureAndEmployeByOffreIdOrderByScoreGlobalDesc(Long offreId);
}