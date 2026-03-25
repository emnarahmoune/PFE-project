package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // ===== RECHERCHES PAR IDENTIFIANTS =====
    Optional<Service> findByCodeService(String codeService);
    boolean existsByCodeService(String codeService);

    // ===== RECHERCHES PAR ATTRIBUTS =====
    List<Service> findByLibelleContainingIgnoreCase(String libelle);
    List<Service> findByActifTrue();
    List<Service> findByActifFalse();

    @Query("SELECT s FROM Service s WHERE s.matriculeResponsable = :matricule")
    List<Service> findByMatriculeResponsable(@Param("matricule") String matricule);

    // ===== RECHERCHES PAR BUDGET =====
    @Query("SELECT s FROM Service s WHERE s.budgetAnnuel IS NOT NULL")
    List<Service> findServicesAvecBudget();

    @Query("SELECT s FROM Service s WHERE s.budgetAnnuel > :budgetMin")
    List<Service> findByBudgetAnnuelGreaterThan(@Param("budgetMin") Double budgetMin);

    @Query("SELECT s FROM Service s WHERE s.budgetAnnuel < :budgetMax")
    List<Service> findByBudgetAnnuelLessThan(@Param("budgetMax") Double budgetMax);

    // ===== RECHERCHES PAR EFFECTIF =====
    @Query("SELECT s FROM Service s WHERE s.effectifCible IS NOT NULL")
    List<Service> findServicesAvecEffectifCible();

    @Query("SELECT s FROM Service s WHERE s.effectifCible > :effectifMin")
    List<Service> findByEffectifCibleGreaterThan(@Param("effectifMin") Integer effectifMin);

    // ===== RECHERCHES PAR DATE =====
    List<Service> findByDateCreationAfter(LocalDateTime date);
    List<Service> findByDateCreationBefore(LocalDateTime date);
    List<Service> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT s FROM Service s ORDER BY s.dateCreation DESC")
    List<Service> findServicesRecents();

    // ===== STATISTIQUES SUR LES EMPLOYÉS =====
    @Query("SELECT s, SIZE(s.employes) FROM Service s ORDER BY SIZE(s.employes) DESC")
    List<Object[]> findServicesByTailleDesc();

    @Query("SELECT s, SIZE(s.employes) FROM Service s ORDER BY SIZE(s.employes) ASC")
    List<Object[]> findServicesByTailleAsc();

    @Query("SELECT s FROM Service s WHERE SIZE(s.employes) = 0")
    List<Service> findServicesVides();

    @Query("SELECT s FROM Service s WHERE SIZE(s.employes) > 0")
    List<Service> findServicesAvecEmployes();

    @Query("SELECT s FROM Service s WHERE SIZE(s.employes) > (SELECT AVG(SIZE(s2.employes)) FROM Service s2)")
    List<Service> findServicesAuDessusMoyenne();

    @Query("SELECT s FROM Service s WHERE SIZE(s.employes) < (SELECT AVG(SIZE(s2.employes)) FROM Service s2)")
    List<Service> findServicesEnDessousMoyenne();

    // ===== STATISTIQUES SUR LES EMPLOYÉS ACTIFS =====
    @Query("SELECT s, COUNT(e) FROM Service s LEFT JOIN s.employes e WHERE e.statut = 'ACTIF' GROUP BY s")
    List<Object[]> countEmployesActifsByService();

    @Query("SELECT s.libelle, COUNT(e) FROM Service s LEFT JOIN s.employes e WHERE e.statut = 'ACTIF' GROUP BY s")
    List<Object[]> countEmployesActifsByServiceLibelle();

    // ===== CALCULS FINANCIERS =====
    @Query("SELECT s, SUM(e.salaire) FROM Service s JOIN s.employes e WHERE e.statut = 'ACTIF' GROUP BY s")
    List<Object[]> calculateMasseSalarialeByService();

    @Query("SELECT s.codeService, s.libelle, SUM(e.salaire) as masseSalariale, s.budgetAnnuel " +
            "FROM Service s JOIN s.employes e WHERE e.statut = 'ACTIF' GROUP BY s")
    List<Object[]> calculateMasseSalarialeAvecBudget();

    @Query("SELECT s FROM Service s WHERE s.budgetAnnuel IS NOT NULL AND " +
            "(SELECT SUM(e.salaire) FROM s.employes e WHERE e.statut = 'ACTIF') > s.budgetAnnuel")
    List<Service> findServicesDepassantBudget();

    @Query("SELECT s.codeService, s.libelle, s.budgetAnnuel, COALESCE(SUM(e.salaire), 0) as masseSalariale " +
            "FROM Service s LEFT JOIN s.employes e ON e.statut = 'ACTIF' GROUP BY s")
    List<Object[]> calculateMasseSalarialeSimple();

    // ===== STATISTIQUES GLOBALES =====
    @Query("SELECT AVG(SIZE(s.employes)) FROM Service s")
    Double moyenneEmployesParService();

    @Query("SELECT SUM(SIZE(s.employes)) FROM Service s")
    Long totalEmployesTousServices();

    @Query("SELECT COUNT(s) FROM Service s WHERE s.actif = true")
    long countServicesActifs();

    @Query("SELECT COUNT(s) FROM Service s")
    long countTotalServices();

    // ===== RECHERCHE AVANCÉE =====
    @Query("SELECT s FROM Service s WHERE " +
            "LOWER(s.codeService) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.libelle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.matriculeResponsable) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Service> searchServices(@Param("keyword") String keyword);

    // ===== TABLEAU DE BORD =====
    @Query("SELECT new map(" +
            "COUNT(s) as totalServices, " +
            "SUM(CASE WHEN s.actif = true THEN 1 ELSE 0 END) as servicesActifs, " +
            "AVG(SIZE(s.employes)) as tailleMoyenne, " +
            "SUM(SIZE(s.employes)) as totalEmployes, " +
            "COUNT(CASE WHEN SIZE(s.employes) = 0 THEN 1 END) as servicesVides, " +
            "AVG(s.budgetAnnuel) as budgetMoyen) " +
            "FROM Service s")
    List<Object[]> getStatsTableauBord();

    // ===== GESTION =====
    @Query("UPDATE Service s SET s.actif = false WHERE s.id = :id")
    void desactiverService(@Param("id") Long id);

    @Query("UPDATE Service s SET s.actif = true WHERE s.id = :id")
    void activerService(@Param("id") Long id);
}