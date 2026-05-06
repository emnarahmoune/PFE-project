package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeCompetence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
@Query("SELECT e FROM Employe e WHERE LOWER(TRIM(e.email)) = LOWER(TRIM(:email))")
Optional<Employe> findByEmailIgnoreCase(@Param("email") String email);


    // ===== RECHERCHES PAR TYPE =====
    List<Employe> findByRoleIn(List<String> roles);

    List<Employe> findByRole(String role);
    
    @Query("SELECT e FROM Employe e WHERE e.role = 'ADMIN_RH'")
    List<Employe> findAllAdminRH();
   
   
    @Query("SELECT e FROM Employe e WHERE e.manager IS NULL AND e.role = 'EMPLOYE'")
    List<Employe> findEmployesSansManager();

    // ===== RECHERCHES PAR ATTRIBUTS =====
    List<Employe> findByDepartement(String departement);
    List<Employe> findByStatut(String statut);
    List<Employe> findByPoste(String poste);
    List<Employe> findByActif(Boolean actif);

    // ✅ AJOUT : Récupère tous les employés actifs (pour findActifs())
    default List<Employe> findAllActifs() {
        return findByActif(true);
    }

    // ✅ AJOUT : Récupère les employés d'un service
    List<Employe> findByServiceId(Long serviceId);

    // ===== RECHERCHES PAR SALAIRE =====
    List<Employe> findBySalaireGreaterThan(Double salaireMin);
    List<Employe> findBySalaireLessThan(Double salaireMax);
    List<Employe> findBySalaireBetween(Double salaireMin, Double salaireMax);

    // ===== RECHERCHES PAR DATE =====
    List<Employe> findByDateEmbaucheAfter(LocalDate date);
    List<Employe> findByDateEmbaucheBefore(LocalDate date);
    @Query("SELECT e FROM Employe e ORDER BY e.dateEmbauche DESC")
    List<Employe> findEmployesRecents();

    // ===== RECHERCHES PAR CONGÉS =====
    List<Employe> findBySoldeCongesLessThan(Integer seuil);
    List<Employe> findBySoldeCongesGreaterThan(Integer seuil);

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

    Optional<Employe> findByEmail(String email);

    // ===== AUTRES =====
    List<Employe> findByManagerEmail(String email);

    @Query("SELECT e FROM Employe e WHERE e.manager IS NULL AND e.role = 'EMPLOYE'")
    List<Employe> findByManagerIsNull();


    


 @Modifying(clearAutomatically = true, flushAutomatically = true)
@Query(
        value = """
                UPDATE employes
                SET role = :role,
                    type_employe = :typeEmploye
                WHERE id = :id
                """,
        nativeQuery = true
)
void updateRoleAndTypeEmploye(
        @Param("id") Long id,
        @Param("role") String role,
        @Param("typeEmploye") String typeEmploye
);

    @Query("SELECT COUNT(e) FROM Employe e")
    int countAllEmployes();

    List<Employe> findByManager_Email(String email);


    @Query("SELECT e FROM Employe e WHERE e.manager.id = :managerId")
List<Employe> findByManagerId(@Param("managerId") Long managerId);

    List<Employe> findByRoleIgnoreCase(String role);

    @Query("SELECT e FROM Employe e WHERE e.manager.id = :managerId AND e.statut = 'ACTIF'")
    List<Employe> findActifsByManagerId(@Param("managerId") Long managerId);


    

@Query("""
    SELECT e
    FROM Employe e
    WHERE UPPER(e.role) = 'MANAGER'
""")
List<Employe> findAllManagers();

}