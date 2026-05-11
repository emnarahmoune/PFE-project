package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.PosteCompetence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PosteCompetenceRepository extends JpaRepository<PosteCompetence, Long> {

    @Query("""
        SELECT pc
        FROM PosteCompetence pc
        JOIN FETCH pc.competence
        WHERE LOWER(pc.poste) = LOWER(:poste)
        """)
    List<PosteCompetence> findByPosteIgnoreCaseWithCompetence(@Param("poste") String poste);

    @Query("""
        SELECT pc
        FROM PosteCompetence pc
        JOIN FETCH pc.competence
        WHERE LOWER(pc.poste) LIKE LOWER(CONCAT('%', :poste, '%'))
           OR LOWER(:poste) LIKE LOWER(CONCAT('%', pc.poste, '%'))
        """)
    List<PosteCompetence> findMatchingPosteWithCompetence(@Param("poste") String poste);
}