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

/**
 * Repository AdministrateurRH
 *
 * Après refactoring : AdministrateurRH étend Utilisateur (stratégie JOINED).
 * Les champs nom/prenom/email sont donc des attributs DIRECTS de AdministrateurRH
 * (hérités de Utilisateur) → les chemins JPQL sont "a.nom", "a.email", etc.
 *
 * L'ancienne syntaxe "a.employe.utilisateur.nom" était fausse car :
 *  - employe.utilisateur pointe vers un Utilisateur générique (type Employe-side)
 *  - AdministrateurRH EST lui-même un Utilisateur
 */
@Repository
public interface AdministrateurRHRepository extends JpaRepository<AdministrateurRH, Long> {

    // ===== RECHERCHES PAR RELATION EMPLOYE =====

    Optional<AdministrateurRH> findByEmployeId(Long employeId);

    @Query("SELECT a FROM AdministrateurRH a WHERE a.employe.matricule = :matricule")
    Optional<AdministrateurRH> findByEmployeMatricule(@Param("matricule") String matricule);

    boolean existsByEmployeId(Long employeId);

    // ===== RECHERCHES PAR EMAIL (direct, hérité de Utilisateur) =====

    /**
     * Cherche par email — a.email est un champ direct (hérité de Utilisateur)
     * Ancienne version erronée : a.employe.utilisateur.email
     */
    @Query("SELECT a FROM AdministrateurRH a WHERE a.email = :email")
    Optional<AdministrateurRH> findByEmail(@Param("email") String email);

    /**
     * Vérifie si un email est celui d'un administrateur
     * Ancienne version erronée : a.employe.utilisateur.email
     */
    @Query("SELECT COUNT(a) > 0 FROM AdministrateurRH a WHERE a.email = :email")
    boolean isEmailAdministrateur(@Param("email") String email);

    // ===== CHARGEMENT AVEC DÉTAILS =====

    /**
     * Charge tous les admins avec leur employe associé.
     * Plus besoin de JOIN FETCH utilisateur car l'admin EST un utilisateur.
     */
    @Query("SELECT a FROM AdministrateurRH a LEFT JOIN FETCH a.employe")
    List<AdministrateurRH> findAllWithDetails();

    // ===== PAGINATION =====

    @Query(value    = "SELECT a FROM AdministrateurRH a LEFT JOIN FETCH a.employe",
            countQuery = "SELECT COUNT(a) FROM AdministrateurRH a")
    Page<AdministrateurRH> findAllWithDetailsPaged(Pageable pageable);

    // ===== STATISTIQUES =====

    @Query("SELECT COUNT(a) FROM AdministrateurRH a")
    long countAdministrateurs();

    // ===== RECHERCHES PAR NOM / PRENOM (champs directs hérités) =====

    /**
     * Ancienne version erronée : a.employe.utilisateur.nom
     * Corrigée : a.nom (hérité de Utilisateur)
     */
    @Query("SELECT a FROM AdministrateurRH a " +
            "WHERE LOWER(a.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<AdministrateurRH> findByNomContaining(@Param("nom") String nom);

    @Query("SELECT a FROM AdministrateurRH a " +
            "WHERE LOWER(a.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<AdministrateurRH> findByPrenomContaining(@Param("prenom") String prenom);

    // ===== RECHERCHE GLOBALE =====

    /**
     * Recherche multi-champs.
     * Ancienne version erronée : a.employe.utilisateur.nom / .prenom / .email
     * Corrigée : a.nom, a.prenom, a.email (tous hérités de Utilisateur)
     * + a.employe.matricule reste valide (relation ManyToOne)
     */
    @Query("SELECT a FROM AdministrateurRH a LEFT JOIN a.employe e WHERE " +
            "LOWER(a.nom)    LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.email)  LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.matricule) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<AdministrateurRH> searchAdministrateurs(@Param("keyword") String keyword);
}