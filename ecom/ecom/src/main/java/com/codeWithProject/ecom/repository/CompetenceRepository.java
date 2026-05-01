package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Competence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompetenceRepository extends JpaRepository<Competence, Long> {

    // ===== RECHERCHES PAR ATTRIBUTS =====
    Optional<Competence> findByNom(String nom);

    boolean existsByNom(String nom);

    List<Competence> findByCategorie(String categorie);

    @Query("SELECT DISTINCT c.categorie FROM Competence c")
    List<String> findAllCategories();


    @Query("SELECT c FROM Competence c LEFT JOIN FETCH c.employes WHERE c.id = :id")
    Optional<Competence> findByIdWithEmployes(Long id);

    // ===== RECHERCHES AVANCÉES =====
    @Query("SELECT c FROM Competence c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Competence> searchCompetences(@Param("keyword") String keyword);

    @Query("SELECT c FROM Competence c ORDER BY c.nom ASC")
    List<Competence> findAllOrderByNomAsc();

    // ===== STATISTIQUES =====
    @Query("SELECT c.categorie, COUNT(c) FROM Competence c GROUP BY c.categorie")
    List<Object[]> countByCategorie();

    @Query("SELECT COUNT(c) FROM Competence c")
    long countTotalCompetences();

    // ===== RECHERCHES SUR LES RELATIONS =====
    @Query("SELECT c FROM Competence c WHERE SIZE(c.employeCompetences) = 0")
    List<Competence> findCompetencesNonAttribuees();

    @Query("SELECT c, COUNT(ec) FROM Competence c LEFT JOIN c.employeCompetences ec GROUP BY c ORDER BY COUNT(ec) DESC")
    List<Object[]> findCompetencesLesPlusUtilisees();

    @Query("SELECT c FROM Competence c WHERE SIZE(c.employeCompetences) = (SELECT MAX(SIZE(ec)) FROM Competence c2 JOIN c2.employeCompetences ec)")
    List<Competence> findCompetencesLesPlusCourantes();
}