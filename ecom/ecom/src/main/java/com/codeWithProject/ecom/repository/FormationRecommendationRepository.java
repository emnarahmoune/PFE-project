package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.FormationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FormationRecommendationRepository extends JpaRepository<FormationRecommendation, Long> {

    List<FormationRecommendation> findByEmployeIdOrderByScoreDesc(Long employeId);

    List<FormationRecommendation> findByEmployeIdAndOffreIdOrderByScoreDesc(Long employeId, Long offreId);

    @Transactional
    void deleteByEmployeIdAndOffreId(Long employeId, Long offreId);

    @Transactional
    void deleteByEmployeIdAndOffreIdIsNullAndType(Long employeId, String type);
}