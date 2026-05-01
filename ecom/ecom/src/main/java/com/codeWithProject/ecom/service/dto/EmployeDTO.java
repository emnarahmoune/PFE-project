package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeDTO {

    // ── Identifiants ──────────────────────────────
    private Long id;
    private String matricule;

    // ── Informations personnelles ─────────────────
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String password;

    // ── Informations professionnelles ─────────────
    private LocalDate dateEmbauche;
    private String poste;
    private Double salaire;
    private String statut;
    private String departement;
    private Integer soldeConges;

    // ── Informations de connexion ─────────────────
    private Boolean actif;
    private LocalDateTime derniereConnexion;
    private Integer nombreConnexions;
    private Boolean compteVerrouille;
    private LocalDateTime dateVerrouillage;

    // ── Rôle Spring Security ──────────────────────
    private String role;

    // ── Type d'employé ────────────────────────────
    private String typeEmploye;

    // ── Relations ─────────────────────────────────
    private Long serviceId;
    private String serviceCode;
    private String serviceLibelle;

    private Long managerId;
    private String managerNom;
    private String managerEmail;      // ← unique déclaration
    private String managerMatricule;

    // ── Statistiques ──────────────────────────────
    private Long anciennete;
    private Double salaireAnnuel;
    private Integer nombreCompetences;
    private Integer nombreFormations;
    private Integer nombreDemandesConge;
    private Integer soldeCongesRestant;
    private String adresse;

    // ── IDs pour les relations ────────────────────
    private List<Long> competenceIds;
    private List<Long> formationIds;
    private List<Long> demandeCongeIds;

    // ── Métadonnées d'affichage ───────────────────
    private String nomComplet;
    private String statutCompte;
    private boolean peutSeConnecter;
    private String statutCouleur;

    // 🔥 NOUVEAU
private List<CompetenceEmployeDTO> competences;
private List<FormationEmployeDTO> formations;
}