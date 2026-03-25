package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.IndicateurRH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicateurRHRepository extends JpaRepository<IndicateurRH, Long> {

    // ===== RECHERCHES PAR TYPE =====
    List<IndicateurRH> findByType(String type);

    @Query("SELECT i FROM IndicateurRH i WHERE i.type = :type ORDER BY i.dateCalcul DESC")
    List<IndicateurRH> findDerniersIndicateursByType(@Param("type") String type);

    @Query("SELECT i FROM IndicateurRH i WHERE i.type = :type AND i.dateCalcul = (SELECT MAX(i2.dateCalcul) FROM IndicateurRH i2 WHERE i2.type = :type)")
    Optional<IndicateurRH> findDernierIndicateurByType(@Param("type") String type);

    // ===== RECHERCHES PAR PÉRIODE =====
    List<IndicateurRH> findByPeriode(String periode);

    @Query("SELECT i FROM IndicateurRH i WHERE i.annee = :annee")
    List<IndicateurRH> findByAnnee(@Param("annee") Integer annee);

    // ===== RECHERCHES PAR DÉPARTEMENT =====
    List<IndicateurRH> findByDepartement(String departement);

    // ===== RECHERCHES PAR DATE =====
    List<IndicateurRH> findByDateCalculBetween(LocalDate debut, LocalDate fin);

    @Query("SELECT i FROM IndicateurRH i WHERE i.dateCalcul >= :date ORDER BY i.dateCalcul DESC")
    List<IndicateurRH> findIndicateursRecents(@Param("date") LocalDate date);

    // ===== RECHERCHES PAR TENDANCE =====
    @Query("SELECT i FROM IndicateurRH i WHERE i.tendance = 'HAUSSE' AND i.type = :type")
    List<IndicateurRH> findEnAmelioration(@Param("type") String type);

    @Query("SELECT i FROM IndicateurRH i WHERE i.tendance = 'BAISSE' AND i.type = :type")
    List<IndicateurRH> findEnDegradation(@Param("type") String type);

    // ===== STATISTIQUES =====
    @Query("SELECT i.type, AVG(i.valeur) FROM IndicateurRH i GROUP BY i.type")
    List<Object[]> moyenneValeursByType();

    // ===== ALERTES =====
    @Query("SELECT i FROM IndicateurRH i WHERE i.valeur > :seuil AND i.type IN ('TURNOVER', 'ABSENTEISME')")
    List<IndicateurRH> findAlertesRouges(@Param("seuil") Double seuil);

    // ===== DERNIERS INDICATEURS =====
    @Query("SELECT i.type, i.valeur, i.tendance, i.dateCalcul FROM IndicateurRH i WHERE i.dateCalcul = (SELECT MAX(i2.dateCalcul) FROM IndicateurRH i2 WHERE i2.type = i.type)")
    List<Object[]> getDerniersIndicateurs();

    // ===== TABLEAU DE BORD =====
    @Query("SELECT new map(" +
            "COUNT(i) as totalIndicateurs, " +
            "COUNT(DISTINCT i.type) as typesIndicateurs, " +
            "AVG(i.valeur) as moyenneGlobale, " +
            "MAX(i.dateCalcul) as dernierCalcul) " +
            "FROM IndicateurRH i")
    List<Object[]> getStatsTableauBord();
}