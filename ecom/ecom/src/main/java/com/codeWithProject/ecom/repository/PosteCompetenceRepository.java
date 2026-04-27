package com.codeWithProject.ecom.repository;
import com.codeWithProject.ecom.entity.PosteCompetence;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codeWithProject.ecom.entity.PosteCompetence;
import java.util.List;
public interface PosteCompetenceRepository
        extends JpaRepository<PosteCompetence, Long> {

    List<PosteCompetence> findByPoste(String poste);
} 
    

