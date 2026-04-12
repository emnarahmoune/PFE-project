package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {

    // ===== RECHERCHES PAR IDENTIFIANTS =====

    @Query("SELECT m FROM Manager m WHERE m.id = :id")
    Optional<Manager> findById(@Param("id") Long id);

    Optional<Manager> findByEmployeId(Long employeId);

    @Query("SELECT m FROM Manager m WHERE m.employe.matricule = :matricule")
    Optional<Manager> findByEmployeMatricule(@Param("matricule") String matricule);

    // ✅ CORRECTION: Manager hérite de Utilisateur, donc email est direct
    Optional<Manager> findByEmail(String email);

    // ===== RECHERCHES PAR DÉPARTEMENT =====
    List<Manager> findByDepartement(String departement);
    List<Manager> findByDepartementContainingIgnoreCase(String departement);
    boolean existsByDepartement(String departement);

    @Query("SELECT m FROM Manager m WHERE m.departement = :departement AND m.actif = true")
    Optional<Manager> findManagerActifByDepartement(@Param("departement") String departement);

    // ===== RECHERCHES PAR DATE =====
    List<Manager> findByDateNominationAfter(LocalDate date);
    List<Manager> findByDateNominationBefore(LocalDate date);
    List<Manager> findByDateNominationBetween(LocalDate debut, LocalDate fin);

    @Query("SELECT m FROM Manager m ORDER BY m.dateNomination DESC")
    List<Manager> findAllByOrderByDateNominationDesc();

    // ===== RECHERCHES PAR STATUT =====
    @Query("SELECT m FROM Manager m WHERE m.actif = true")
    List<Manager> findManagersActifs();

    @Query("SELECT m FROM Manager m WHERE m.actif = false")
    List<Manager> findManagersInactifs();

    // ===== REQUÊTES SUR LES ÉQUIPES =====
    @Query("SELECT m, SIZE(m.employesGeres) FROM Manager m ORDER BY SIZE(m.employesGeres) DESC")
    List<Object[]> findManagersByTailleEquipeDesc();

    @Query("SELECT m FROM Manager m ORDER BY SIZE(m.employesGeres) DESC")
    List<Manager> findManagersTriesParTailleEquipe();

    @Query("SELECT m FROM Manager m WHERE m.employesGeres IS EMPTY")
    List<Manager> findManagersSansEquipe();

    @Query("SELECT m FROM Manager m WHERE SIZE(m.employesGeres) > 0")
    List<Manager> findManagersAvecEquipe();

    @Query("SELECT m, COUNT(e) FROM Manager m LEFT JOIN m.employesGeres e GROUP BY m")
    List<Object[]> countEmployesByManager();

    @Query("SELECT AVG(SIZE(m.employesGeres)) FROM Manager m")
    Double calculateTailleMoyenneEquipe();

    @Query("SELECT m FROM Manager m GROUP BY m HAVING SIZE(m.employesGeres) > (SELECT AVG(SIZE(m2.employesGeres)) FROM Manager m2)")
    List<Manager> findManagersAvecGrandeEquipe();

    // ===== REQUÊTES SUR LES DEMANDES DE CONGÉ =====
    @Query("SELECT DISTINCT m FROM Manager m JOIN m.demandesCongeAValider d WHERE d.statut = 'EN_ATTENTE'")
    List<Manager> findManagersAvecDemandesEnAttente();

    @Query("SELECT m, COUNT(d) FROM Manager m LEFT JOIN m.demandesCongeAValider d WHERE d.statut = 'EN_ATTENTE' GROUP BY m")
    List<Object[]> countDemandesEnAttenteByManager();

    @Query("SELECT DISTINCT m FROM Manager m JOIN m.demandesCongeAValider d WHERE d.statut = 'EN_ATTENTE' AND d.urgente = true")
    List<Manager> findManagersAvecDemandesUrgentes();

    @Query("SELECT DISTINCT m FROM Manager m JOIN m.demandesCongeAValider d WHERE d.statut = 'EN_ATTENTE' AND d.dateDebut <= :dateLimite")
    List<Manager> findManagersAvecDemandesUrgentesAvantDate(@Param("dateLimite") LocalDate dateLimite);

    // ===== RECHERCHES PAR NOM =====
    @Query("SELECT m FROM Manager m WHERE LOWER(m.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Manager> findByNomContaining(@Param("nom") String nom);

    @Query("SELECT m FROM Manager m WHERE LOWER(m.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<Manager> findByPrenomContaining(@Param("prenom") String prenom);

    @Query("SELECT m FROM Manager m WHERE LOWER(m.nom) LIKE LOWER(CONCAT('%', :nom, '%')) AND LOWER(m.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<Manager> findByNomAndPrenom(@Param("nom") String nom, @Param("prenom") String prenom);

    // ===== RECHERCHE GLOBALE =====
    @Query("SELECT m FROM Manager m WHERE " +
            "LOWER(m.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.departement) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Manager> searchManagers(@Param("keyword") String keyword);

    // ===== STATISTIQUES =====
    @Query("SELECT new map(" +
            "COUNT(m) as totalManagers, " +
            "SUM(CASE WHEN m.actif = true THEN 1 ELSE 0 END) as managersActifs, " +
            "AVG(SIZE(m.employesGeres)) as tailleMoyenneEquipe, " +
            "SUM(SIZE(m.employesGeres)) as totalEmployesGeres, " +
            "COUNT(DISTINCT m.departement) as departementsCouverts) " +
            "FROM Manager m")
    List<Object[]> getManagersStats();

    @Query("SELECT m.departement, COUNT(m) FROM Manager m GROUP BY m.departement")
    List<Object[]> countManagersByDepartement();

    @Query(value = "SELECT AVG(DATEDIFF(CURDATE(), date_nomination) / 365.25) FROM managers WHERE date_nomination IS NOT NULL", nativeQuery = true)
    Double calculateAncienneteMoyenne();

    @Query("SELECT m FROM Manager m WHERE m.dateNomination IS NOT NULL ORDER BY m.dateNomination DESC")
    List<Manager> findManagersRecents();

    @Query("SELECT m FROM Manager m WHERE m.dateNomination IS NOT NULL ORDER BY m.dateNomination ASC")
    List<Manager> findManagersAnciens();

    // ===== TABLEAU DE BORD =====
    @Query("SELECT m.departement, COUNT(e) as employes, COUNT(d) as demandes FROM Manager m " +
            "LEFT JOIN m.employesGeres e LEFT JOIN m.demandesCongeAValider d " +
            "GROUP BY m.departement, m.id")
    List<Object[]> getStatsManagers();

    // Dans ManagerRepository.java - AJOUTER cette méthode
    @Query("SELECT m FROM Manager m WHERE m.email = :email")
    Optional<Manager> findByUtilisateurEmail(@Param("email") String email);

    // ✅ Garder cette méthode pour le manager par défaut
    Optional<Manager> findFirstByOrderByIdAsc();
}