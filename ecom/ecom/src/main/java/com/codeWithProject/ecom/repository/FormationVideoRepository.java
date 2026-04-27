package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.FormationVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormationVideoRepository extends JpaRepository<FormationVideo, Long> {

    long countByFormationId(Long formationId);
    List<FormationVideo> findByFormationId(Long formationId);

    List<FormationVideo> findByFormation_IdOrderByOrdre(Long formationId);

}