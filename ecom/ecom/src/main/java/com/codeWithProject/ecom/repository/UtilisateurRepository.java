package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // ===== RECHERCHES PAR IDENTIFIANTS =====
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM Utilisateur u WHERE u.employe.id = :employeId")
    Optional<Utilisateur> findByEmployeId(@Param("employeId") Long employeId);

    // ===== RECHERCHES PAR STATUT =====
    List<Utilisateur> findByActifTrue();
    List<Utilisateur> findByActifFalse();

    List<Utilisateur> findByCompteVerrouilleTrue();
    List<Utilisateur> findByCompteVerrouilleFalse();

    @Query("SELECT u FROM Utilisateur u WHERE u.actif = true AND u.compteVerrouille = false")
    List<Utilisateur> findUtilisateursActifsEtNonVerrouilles();

    // ===== RECHERCHES PAR TYPE =====
    List<Utilisateur> findByTypeUtilisateur(String typeUtilisateur);

    @Query("SELECT u FROM Utilisateur u WHERE TYPE(u) = Manager")
    List<Utilisateur> findManagers();

    @Query("SELECT u FROM Utilisateur u WHERE u.employe IS NOT NULL")
    List<Utilisateur> findEmployesAvecCompte();

    @Query("SELECT u FROM Utilisateur u WHERE u.employe IS NULL")
    List<Utilisateur> findUtilisateursSansEmploye();

    // ===== RECHERCHES PAR DATE =====
    List<Utilisateur> findByDateCreationAfter(LocalDateTime date);
    List<Utilisateur> findByDateCreationBefore(LocalDateTime date);
    List<Utilisateur> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT u FROM Utilisateur u ORDER BY u.dateCreation DESC")
    List<Utilisateur> findUtilisateursRecents();

    // ===== RECHERCHES PAR CONNEXION =====
    List<Utilisateur> findByDerniereConnexionAfter(LocalDateTime date);
    List<Utilisateur> findByDerniereConnexionBefore(LocalDateTime date);

    @Query("SELECT u FROM Utilisateur u WHERE u.derniereConnexion IS NULL")
    List<Utilisateur> findUtilisateursJamaisConnectes();

    @Query("SELECT u FROM Utilisateur u WHERE u.derniereConnexion < :dateLimite")
    List<Utilisateur> findUtilisateursInactifs(@Param("dateLimite") LocalDateTime dateLimite);

    @Query("SELECT u FROM Utilisateur u WHERE u.derniereConnexion >= :date ORDER BY u.derniereConnexion DESC")
    List<Utilisateur> findUtilisateursActifsRecents(@Param("date") LocalDateTime date);

    // ===== RECHERCHES PAR TENTATIVES DE CONNEXION =====
    List<Utilisateur> findByTentativesEchecGreaterThanEqual(Integer seuil);

    @Query("SELECT u FROM Utilisateur u WHERE u.tentativesEchec >= 5")
    List<Utilisateur> findUtilisateursAvecTropTentatives();

    // ===== RECHERCHES PAR VERROUILLAGE =====
    List<Utilisateur> findByDateVerrouillageAfter(LocalDateTime date);
    List<Utilisateur> findByDateVerrouillageBefore(LocalDateTime date);

    @Query("SELECT u FROM Utilisateur u WHERE u.compteVerrouille = true AND u.dateVerrouillage < :dateLimite")
    List<Utilisateur> findVerrouillesAvant(@Param("dateLimite") LocalDateTime dateLimite);

    // ===== RECHERCHES AVANCÉES =====
    @Query("SELECT u FROM Utilisateur u WHERE " +
            "LOWER(u.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Utilisateur> searchUtilisateurs(@Param("keyword") String keyword);

    @Query("SELECT u FROM Utilisateur u WHERE LOWER(u.nom) = LOWER(:nom) AND LOWER(u.prenom) = LOWER(:prenom)")
    List<Utilisateur> findByNomAndPrenom(@Param("nom") String nom, @Param("prenom") String prenom);

    // ===== STATISTIQUES =====
    @Query("SELECT u.typeUtilisateur, COUNT(u) FROM Utilisateur u GROUP BY u.typeUtilisateur")
    List<Object[]> countByTypeUtilisateur();

    @Query("SELECT u.actif, COUNT(u) FROM Utilisateur u GROUP BY u.actif")
    List<Object[]> countByActif();

    @Query("SELECT u.compteVerrouille, COUNT(u) FROM Utilisateur u GROUP BY u.compteVerrouille")
    List<Object[]> countByCompteVerrouille();

    @Query("SELECT AVG(u.nombreConnexions) FROM Utilisateur u")
    Double moyenneConnexions();

    @Query("SELECT COUNT(u) FROM Utilisateur u WHERE u.derniereConnexion IS NOT NULL")
    long countUtilisateursConnectesAuMoinsUneFois();

    // ===== MISES À JOUR =====
    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.derniereConnexion = :date, u.nombreConnexions = u.nombreConnexions + 1, u.tentativesEchec = 0 WHERE u.id = :id")
    void updateConnexionReussie(@Param("id") Long id, @Param("date") LocalDateTime date);

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.tentativesEchec = u.tentativesEchec + 1 WHERE u.id = :id")
    void incrementerTentativesEchec(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.compteVerrouille = true, u.dateVerrouillage = :date WHERE u.id = :id")
    void verrouillerCompte(@Param("id") Long id, @Param("date") LocalDateTime date);

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.compteVerrouille = false, u.dateVerrouillage = null, u.tentativesEchec = 0 WHERE u.id = :id")
    void deverrouillerCompte(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.actif = true WHERE u.id = :id")
    void activerCompte(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.actif = false WHERE u.id = :id")
    void desactiverCompte(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Utilisateur u SET u.tentativesEchec = 0 WHERE u.id = :id")
    void resetTentativesEchec(@Param("id") Long id);

    // ===== TABLEAU DE BORD =====
    @Query("SELECT new map(" +
            "COUNT(u) as totalUtilisateurs, " +
            "SUM(CASE WHEN u.actif = true THEN 1 ELSE 0 END) as utilisateursActifs, " +
            "SUM(CASE WHEN u.compteVerrouille = true THEN 1 ELSE 0 END) as comptesVerrouilles, " +
            "SUM(CASE WHEN u.derniereConnexion IS NULL THEN 1 ELSE 0 END) as jamaisConnectes, " +
            "AVG(u.nombreConnexions) as moyenneConnexions, " +
            "MAX(u.derniereConnexion) as derniereConnexionGlobale) " +
            "FROM Utilisateur u")
    List<Object[]> getStatsTableauBord();
    // 🔥 Ajouter ces méthodes pour les rôles dynamiques
    List<Utilisateur> findByRole(String role);

    List<Utilisateur> findByRoleIgnoreCase(String role);

    boolean existsByRole(String role);
}