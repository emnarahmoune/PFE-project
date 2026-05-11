package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.FormationVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormationVideoRepository extends JpaRepository<FormationVideo, Long> {

    List<FormationVideo> findByFormation_IdOrderByOrdreAsc(Long formationId);

    boolean existsByFormation_Id(Long formationId);

    boolean existsByFormation_IdAndUrlYoutubeContainingIgnoreCase(
            Long formationId,
            String keyword
    );

    void deleteByFormation_IdAndUrlYoutubeContainingIgnoreCase(
            Long formationId,
            String keyword
    );
}