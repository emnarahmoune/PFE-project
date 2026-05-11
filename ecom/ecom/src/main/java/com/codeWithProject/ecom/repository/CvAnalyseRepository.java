package com.codeWithProject.ecom.repository;


import com.codeWithProject.ecom.entity.CvAnalyse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvAnalyseRepository extends JpaRepository<CvAnalyse, Long> {

    Optional<CvAnalyse> findByCandidatureId(Long candidatureId);

    Optional<CvAnalyse> findByOffreIdAndEmployeId(Long offreId, Long employeId);

    boolean existsByCandidatureId(Long candidatureId);

    void deleteByCandidatureId(Long candidatureId);
}