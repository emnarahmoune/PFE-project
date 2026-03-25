package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {

    // ===== RECHERCHES PAR ATTRIBUTS =====
    Optional<Formation> findByTitre(String titre);
    boolean existsByTitre(String titre);

    List<Formation> findByDomaine(String domaine);
    List<Formation> findByDureeHeuresLessThanEqual(Integer dureeMax);
    List<Formation> findByDureeHeuresGreaterThanEqual(Integer dureeMin);

    List<Formation> findByActifTrue();
    List<Formation> findByActifFalse();

    // ===== RECHERCHES PAR DATE =====
    List<Formation> findByDateCreationAfter(LocalDateTime date);
    List<Formation> findByDateCreationBefore(LocalDateTime date);
    List<Formation> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT f FROM Formation f ORDER BY f.dateCreation DESC")
    List<Formation> findFormationsRecentes();

    // ===== RECHERCHES AVANCÉES =====
    @Query("SELECT f FROM Formation f WHERE LOWER(f.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Formation> searchFormations(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT f.domaine FROM Formation f")
    List<String> findAllDomaines();

    // ===== STATISTIQUES =====
    @Query("SELECT f.domaine, COUNT(f) FROM Formation f GROUP BY f.domaine")
    List<Object[]> countByDomaine();

    @Query("SELECT AVG(f.dureeHeures) FROM Formation f")
    Double dureeMoyenneFormations();

    @Query("SELECT COUNT(f) FROM Formation f")
    long countTotalFormations();

    // ===== RECHERCHES SUR LES PARTICIPANTS =====
    @Query("SELECT f, COUNT(p) FROM Formation f LEFT JOIN f.participants p GROUP BY f ORDER BY COUNT(p) DESC")
    List<Object[]> findFormationsLesPlusSuivies();

    @Query("SELECT f FROM Formation f WHERE SIZE(f.participants) = 0")
    List<Formation> findFormationsNonSuivies();

    @Query("SELECT f FROM Formation f WHERE SIZE(f.participants) > (SELECT AVG(SIZE(f2.participants)) FROM Formation f2)")
    List<Formation> findFormationsPopulaires();

    @Query("SELECT f FROM Formation f WHERE SIZE(f.participants) < (SELECT AVG(SIZE(f2.participants)) FROM Formation f2)")
    List<Formation> findFormationsPeuSuivies();

    // ===== RECHERCHES PAR PARTICIPANT =====
    @Query("SELECT f FROM Formation f JOIN f.participants p WHERE p.id = :employeId")
    List<Formation> findFormationsByEmployeId(@Param("employeId") Long employeId);

    @Query("SELECT f FROM Formation f WHERE NOT EXISTS (SELECT p FROM f.participants p WHERE p.id = :employeId)")
    List<Formation> findFormationsNonSuiviesParEmploye(@Param("employeId") Long employeId);

    // ===== TABLEAU DE BORD =====
    @Query("SELECT new map(" +
            "COUNT(f) as totalFormations, " +
            "SUM(CASE WHEN f.actif = true THEN 1 ELSE 0 END) as formationsActives, " +
            "AVG(f.dureeHeures) as dureeMoyenne, " +
            "AVG(SIZE(f.participants)) as participantsMoyens, " +
            "SUM(SIZE(f.participants)) as totalParticipants) " +
            "FROM Formation f")
    List<Object[]> getStatsTableauBord();
}