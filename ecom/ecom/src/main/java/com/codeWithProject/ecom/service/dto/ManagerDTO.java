package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour l'entité Manager
 *
 * Après refactoring : Manager hérite de Employe
 * Donc tous les champs de Employe sont directement dans Manager
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDTO {

    // ── Identifiant ────────────────────────────────
    private Long id;
    private String matricule;

    // ── Champs hérités de Employe ─────────────────
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String poste;
    private Double salaire;
    private String statut;
    private String departement;
    private Integer soldeConges;
    private LocalDate dateEmbauche;          // ✅ AJOUTÉ (manquait)

    // ── Informations de connexion ─────────────────
    private Boolean actif;
    private LocalDateTime derniereConnexion;
    private Integer nombreConnexions;
    private Boolean compteVerrouille;
    private LocalDate dateCreation;

    // ── Rôle ──────────────────────────────────────
    private String role;  // "manager"

    // ── Champs spécifiques Manager ────────────────
    private LocalDate dateNomination;

    // ── Relations ─────────────────────────────────
    private Long employeId;
    private String employeMatricule;
    private String employeNom;
    private String employePrenom;

    // ── Statistiques ──────────────────────────────
    private Integer nombreEmployesGeres;
    private Integer nombreEmployesTotal;
    private Long nombreDemandesEnAttente;
    private List<Long> employesGeresIds;
    private List<Long> demandesEnAttenteIds;

    // ── Métadonnées ───────────────────────────────
    private String nomComplet;
    private Long anciennete;
    private Long ancienneteManager;
    private String statutCompte;
    private boolean peutSeConnecter;

    // ── Pour le tableau de bord ───────────────────
    private Boolean aDesDemandesUrgentes;
    private Integer nombreDemandesUrgentes;
    private Double tauxRemplissageEquipe;
    private String departementManager;
}