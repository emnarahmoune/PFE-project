package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrateurRHRepository extends JpaRepository<AdministrateurRH, Long> {

    // ===== RECHERCHES PAR IDENTIFIANTS =====
    Optional<AdministrateurRH> findByMatricule(String matricule);
    Optional<AdministrateurRH> findByEmail(String email);

    // ✅ AJOUT : recherche par employeId (l'admin étant lui-même un employé, on cherche par son propre ID)
    // Mais attention : AdministrateurRH hérite de Employe, donc son ID est un ID d'employé.
    // La méthode suivante est redondante avec findById, mais gardons-la pour compatibilité.
    default Optional<AdministrateurRH> findByEmployeId(Long employeId) {
        return findById(employeId);
    }

    // ✅ AJOUT : recherche par matricule (le matricule est un champ direct)
    default Optional<AdministrateurRH> findByEmployeMatricule(String matricule) {
        return findByMatricule(matricule);
    }

    // ✅ AJOUT : existence par employeId
    default boolean existsByEmployeId(Long employeId) {
        return existsById(employeId);
    }

    // ===== RECHERCHES PAR NOM/PRENOM =====
    @Query("SELECT a FROM AdministrateurRH a WHERE LOWER(a.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<AdministrateurRH> findByNomContaining(@Param("nom") String nom);

    @Query("SELECT a FROM AdministrateurRH a WHERE LOWER(a.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<AdministrateurRH> findByPrenomContaining(@Param("prenom") String prenom);

    // ===== RECHERCHES PAR STATUT =====
    @Query("SELECT a FROM AdministrateurRH a WHERE a.actif = true")
    List<AdministrateurRH> findAdminsActifs();

    @Query("SELECT a FROM AdministrateurRH a WHERE a.actif = false")
    List<AdministrateurRH> findAdminsInactifs();

    // ===== CHARGEMENT AVEC DÉTAILS =====
    @Query("SELECT a FROM AdministrateurRH a")
    List<AdministrateurRH> findAllWithDetails();

    // ===== PAGINATION =====
    @Query(value = "SELECT a FROM AdministrateurRH a",
            countQuery = "SELECT COUNT(a) FROM AdministrateurRH a")
    Page<AdministrateurRH> findAllWithDetailsPaged(Pageable pageable);

    // ===== STATISTIQUES =====
    @Query("SELECT COUNT(a) FROM AdministrateurRH a")
    long countAdministrateurs();

    @Query("SELECT COUNT(a) FROM AdministrateurRH a WHERE a.actif = true")
    long countAdminsActifs();

    // ===== RECHERCHE GLOBALE =====
    @Query("SELECT a FROM AdministrateurRH a WHERE " +
            "LOWER(a.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.matricule) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<AdministrateurRH> searchAdministrateurs(@Param("keyword") String keyword);

    // ===== VÉRIFICATIONS =====
    @Query("SELECT COUNT(a) > 0 FROM AdministrateurRH a WHERE a.email = :email")
    boolean isEmailAdministrateur(@Param("email") String email);
}