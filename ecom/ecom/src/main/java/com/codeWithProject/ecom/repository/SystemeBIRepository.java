package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.SystemeBI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemeBIRepository extends JpaRepository<SystemeBI, Long> {

    // ===== RECHERCHES PAR VERSION =====
    Optional<SystemeBI> findByVersion(String version);
    boolean existsByVersion(String version);

    // ===== RECHERCHES PAR OUTILS =====
    List<SystemeBI> findByOutilETL(String outilETL);
    List<SystemeBI> findByOutilVisualisation(String outilVisualisation);
    List<SystemeBI> findByModeleML(String modeleML);

    @Query("SELECT DISTINCT s.outilETL FROM SystemeBI s")
    List<String> findAllOutilsETL();

    @Query("SELECT DISTINCT s.outilVisualisation FROM SystemeBI s")
    List<String> findAllOutilsVisualisation();

    // ===== RECHERCHES PAR STATUT =====
    List<SystemeBI> findByStatut(String statut);

    @Query("SELECT s FROM SystemeBI s WHERE s.statut = 'ACTIF'")
    List<SystemeBI> findSystemesActifs();

    @Query("SELECT s FROM SystemeBI s WHERE s.statut = 'MAINTENANCE'")
    List<SystemeBI> findSystemesEnMaintenance();

    @Query("SELECT s FROM SystemeBI s WHERE s.statut = 'INACTIF'")
    List<SystemeBI> findSystemesInactifs();

    // ===== RECHERCHES PAR DATE D'EXÉCUTION =====
    List<SystemeBI> findByDerniereExecutionAfter(LocalDateTime date);
    List<SystemeBI> findByDerniereExecutionBefore(LocalDateTime date);

    @Query("SELECT s FROM SystemeBI s WHERE s.derniereExecution IS NULL")
    List<SystemeBI> findSystemesJamaisExecutes();

    @Query("SELECT s FROM SystemeBI s WHERE s.derniereExecution >= :date ORDER BY s.derniereExecution DESC")
    List<SystemeBI> findSystemesExecutesRecement(@Param("date") LocalDateTime date);

    @Query("SELECT s FROM SystemeBI s ORDER BY s.derniereExecution DESC NULLS LAST")
    List<SystemeBI> findSystemesByDerniereExecutionDesc();

    // ===== STATISTIQUES SUR LES RELATIONS =====
    @Query("SELECT s, SIZE(s.indicateurs) FROM SystemeBI s")
    List<Object[]> countIndicateursBySysteme();

    @Query("SELECT s, SIZE(s.scoresTurnover) FROM SystemeBI s")
    List<Object[]> countScoresBySysteme();

    @Query("SELECT s FROM SystemeBI s WHERE SIZE(s.indicateurs) > 0")
    List<SystemeBI> findSystemesAvecIndicateurs();

    @Query("SELECT s FROM SystemeBI s WHERE SIZE(s.scoresTurnover) > 0")
    List<SystemeBI> findSystemesAvecScores();

    @Query("SELECT s FROM SystemeBI s WHERE SIZE(s.indicateurs) = 0 AND SIZE(s.scoresTurnover) = 0")
    List<SystemeBI> findSystemesSansDonnees();

    // ===== STATISTIQUES GLOBALES =====
    @Query("SELECT s.statut, COUNT(s) FROM SystemeBI s GROUP BY s.statut")
    List<Object[]> countByStatut();

    @Query("SELECT s.outilETL, COUNT(s) FROM SystemeBI s GROUP BY s.outilETL")
    List<Object[]> countByOutilETL();

    @Query("SELECT s.outilVisualisation, COUNT(s) FROM SystemeBI s GROUP BY s.outilVisualisation")
    List<Object[]> countByOutilVisualisation();

    @Query("SELECT s.modeleML, COUNT(s) FROM SystemeBI s GROUP BY s.modeleML")
    List<Object[]> countByModeleML();

    @Query("SELECT COUNT(s) FROM SystemeBI s")
    long countTotalSystemes();

    @Query("SELECT MAX(s.derniereExecution) FROM SystemeBI s")
    LocalDateTime findDerniereExecutionGlobale();

    // ===== RECHERCHE AVANCÉE =====
    @Query("SELECT s FROM SystemeBI s WHERE " +
            "LOWER(s.version) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.outilETL) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.outilVisualisation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.modeleML) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SystemeBI> searchSystemesBI(@Param("keyword") String keyword);

    // ===== TABLEAU DE BORD =====
    @Query("SELECT new map(" +
            "COUNT(s) as totalSystemes, " +
            "SUM(CASE WHEN s.statut = 'ACTIF' THEN 1 ELSE 0 END) as systemesActifs, " +
            "SUM(CASE WHEN s.statut = 'MAINTENANCE' THEN 1 ELSE 0 END) as systemesMaintenance, " +
            "SUM(CASE WHEN s.statut = 'INACTIF' THEN 1 ELSE 0 END) as systemesInactifs, " +
            "SUM(SIZE(s.indicateurs)) as totalIndicateurs, " +
            "SUM(SIZE(s.scoresTurnover)) as totalScores, " +
            "MAX(s.derniereExecution) as derniereExecution) " +
            "FROM SystemeBI s")
    List<Object[]> getStatsTableauBord();

    // ===== MISES À JOUR =====
    @Query("UPDATE SystemeBI s SET s.statut = 'MAINTENANCE' WHERE s.id = :id")
    void mettreEnMaintenance(@Param("id") Long id);

    @Query("UPDATE SystemeBI s SET s.statut = 'ACTIF' WHERE s.id = :id")
    void activer(@Param("id") Long id);

    @Query("UPDATE SystemeBI s SET s.statut = 'INACTIF' WHERE s.id = :id")
    void desactiver(@Param("id") Long id);

    @Query("UPDATE SystemeBI s SET s.derniereExecution = :date WHERE s.id = :id")
    void updateDerniereExecution(@Param("id") Long id, @Param("date") LocalDateTime date);
}