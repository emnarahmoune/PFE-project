package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Competence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetenceRepository extends JpaRepository<Competence, Long> {

    Optional<Competence> findByNom(String nom);

    boolean existsByNom(String nom);

    List<Competence> findByCategorie(String categorie);

    @Query("SELECT DISTINCT c.categorie FROM Competence c WHERE c.categorie IS NOT NULL ORDER BY c.categorie ASC")
    List<String> findAllCategories();

    @Query("""
           SELECT DISTINCT c
           FROM Competence c
           LEFT JOIN FETCH c.employeCompetences ec
           LEFT JOIN FETCH ec.employe
           WHERE c.id = :id
           """)
    Optional<Competence> findByIdWithEmployes(@Param("id") Long id);

    @Query("""
           SELECT c
           FROM Competence c
           WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
           ORDER BY c.nom ASC
           """)
    List<Competence> searchCompetences(@Param("keyword") String keyword);

    @Query("SELECT c FROM Competence c ORDER BY c.nom ASC")
    List<Competence> findAllOrderByNomAsc();

    @Query("""
           SELECT c.categorie, COUNT(c)
           FROM Competence c
           GROUP BY c.categorie
           """)
    List<Object[]> countByCategorie();

    @Query("SELECT COUNT(c) FROM Competence c")
    long countTotalCompetences();

    @Query("""
           SELECT c
           FROM Competence c
           WHERE c.employeCompetences IS EMPTY
           ORDER BY c.nom ASC
           """)
    List<Competence> findCompetencesNonAttribuees();

    @Query("""
           SELECT c, COUNT(ec)
           FROM Competence c
           LEFT JOIN c.employeCompetences ec
           GROUP BY c
           ORDER BY COUNT(ec) DESC
           """)
    List<Object[]> findCompetencesLesPlusUtilisees();

    @Query("""
           SELECT c
           FROM Competence c
           LEFT JOIN c.employeCompetences ec
           GROUP BY c
           ORDER BY COUNT(ec) DESC
           """)
    List<Competence> findCompetencesLesPlusCourantes();
}