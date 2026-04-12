package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {

    // ===== RECHERCHES PAR IDENTIFIANTS =====
    Optional<Employe> findByMatricule(String matricule);
    boolean existsByMatricule(String matricule);
    Optional<Employe> findByEmail(String email);

    // ===== RECHERCHES PAR ATTRIBUTS =====
    List<Employe> findByDepartement(String departement);
    List<Employe> findByStatut(String statut);
    List<Employe> findByPoste(String poste);

    @Query("SELECT e FROM Employe e WHERE LOWER(e.poste) LIKE LOWER(CONCAT('%', :poste, '%'))")
    List<Employe> findByPosteContaining(@Param("poste") String poste);

    // ===== RECHERCHES PAR SALAIRE =====
    List<Employe> findBySalaireGreaterThan(Double salaireMin);
    List<Employe> findBySalaireLessThan(Double salaireMax);
    List<Employe> findBySalaireBetween(Double salaireMin, Double salaireMax);

    @Query("SELECT e FROM Employe e ORDER BY e.salaire DESC")
    List<Employe> findAllOrderBySalaireDesc();

    @Query("SELECT e FROM Employe e ORDER BY e.salaire ASC")
    List<Employe> findAllOrderBySalaireAsc();

    // ===== RECHERCHES PAR DATE =====
    List<Employe> findByDateEmbaucheAfter(LocalDate date);
    List<Employe> findByDateEmbaucheBefore(LocalDate date);
    List<Employe> findByDateEmbaucheBetween(LocalDate debut, LocalDate fin);

    @Query("SELECT e FROM Employe e LEFT JOIN FETCH e.manager ORDER BY e.dateEmbauche DESC")
    List<Employe> findEmployesRecents();

    @Query("SELECT e FROM Employe e ORDER BY e.dateEmbauche ASC")
    List<Employe> findEmployesAnciens();

    // ===== RECHERCHES PAR CONGÉS =====
    List<Employe> findBySoldeCongesLessThan(Integer seuil);
    List<Employe> findBySoldeCongesGreaterThan(Integer seuil);
    List<Employe> findBySoldeConges(Integer solde);

    @Query("SELECT e FROM Employe e WHERE e.soldeConges < 5")
    List<Employe> findEmployesSoldeCongesFaible();

    // ===== RECHERCHES PAR RELATIONS =====
    List<Employe> findByManagerId(Long managerId);
    List<Employe> findByServiceId(Long serviceId);

    @Query("SELECT e FROM Employe e WHERE e.manager IS NULL")
    List<Employe> findEmployesSansManager();

    @Query("SELECT e FROM Employe e WHERE e.service IS NULL")
    List<Employe> findEmployesSansService();

    // ===== RECHERCHES AVANCÉES =====
    @Query("SELECT e FROM Employe e WHERE e.statut = 'ACTIF'")
    List<Employe> findAllActifs();

    @Query("SELECT e FROM Employe e WHERE e.statut = 'INACTIF'")
    List<Employe> findAllInactifs();

    @Query("SELECT e FROM Employe e WHERE e.statut = 'CONGE'")
    List<Employe> findAllEnConge();

    // ===== STATISTIQUES =====
    @Query("SELECT e.departement, COUNT(e) FROM Employe e GROUP BY e.departement")
    List<Object[]> countByDepartement();

    @Query("SELECT e.statut, COUNT(e) FROM Employe e GROUP BY e.statut")
    List<Object[]> countByStatut();

    @Query("SELECT e.poste, COUNT(e) FROM Employe e GROUP BY e.poste")
    List<Object[]> countByPoste();

    @Query("SELECT AVG(e.salaire) FROM Employe e WHERE e.statut = 'ACTIF'")
    Double salaireMoyen();

    @Query("SELECT SUM(e.salaire) FROM Employe e WHERE e.statut = 'ACTIF'")
    Double sommeSalaires();

    @Query("SELECT MIN(e.salaire) FROM Employe e WHERE e.statut = 'ACTIF'")
    Double salaireMinimum();

    @Query("SELECT MAX(e.salaire) FROM Employe e WHERE e.statut = 'ACTIF'")
    Double salaireMaximum();

    @Query("SELECT AVG(e.soldeConges) FROM Employe e WHERE e.statut = 'ACTIF'")
    Double soldeCongesMoyen();

    // ===== RECHERCHE GLOBALE =====
    @Query("SELECT e FROM Employe e WHERE " +
            "LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.matricule) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.poste) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.departement) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Employe> searchEmployes(@Param("keyword") String keyword);

    // ✅ CORRECTION : retourner une List<Map<String, Object>>
    @Query("SELECT new map(" +
            "COUNT(e) as total, " +
            "SUM(CASE WHEN e.statut = 'ACTIF' THEN 1 ELSE 0 END) as actifs, " +
            "SUM(CASE WHEN e.statut = 'INACTIF' THEN 1 ELSE 0 END) as inactifs, " +
            "SUM(CASE WHEN e.statut = 'CONGE' THEN 1 ELSE 0 END) as enConge, " +
            "AVG(e.salaire) as salaireMoyen, " +
            "SUM(e.salaire) as masseSalariale, " +
            "AVG(e.soldeConges) as soldeCongesMoyen) " +
            "FROM Employe e")
    List<Map<String, Object>> getStatsTableauBord();

    // ===== AUTRES =====
    List<Employe> findByManagerEmail(String email);
    // Dans EmployeRepository.java - AJOUTER ces méthodes

    // Dans EmployeRepository.java - AJOUTER cette méthode
    @Query("SELECT e FROM Employe e WHERE e.manager IS NULL")
    List<Employe> findByManagerIsNull();
    // ✅ À AJOUTER pour les employés actifs d'un manager
    @Query("SELECT e FROM Employe e WHERE e.manager.id = :managerId AND e.statut = 'ACTIF'")
    List<Employe> findActifsByManagerId(@Param("managerId") Long managerId);

}