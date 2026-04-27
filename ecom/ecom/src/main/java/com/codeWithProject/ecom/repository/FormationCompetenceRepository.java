package com.codeWithProject.ecom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.codeWithProject.ecom.entity.FormationCompetence;
import java.util.List;

public interface FormationCompetenceRepository
        extends JpaRepository<FormationCompetence, Long> {

    List<FormationCompetence> findByCompetence_Id(Long competenceId);
}
