package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour l'entité AdministrateurRH
 *
 * Après refactoring : AdministrateurRH hérite de Employe (qui contient tous les champs communs)
 * Donc les champs nom, prenom, email, etc. sont directement dans AdministrateurRH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministrateurRHDTO {

    // ── Identifiant ────────────────────────────────
    private Long id;

    // ── Champs hérités de Employe ─────────────────
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String poste;
    private String departement;
    private Double salaire;
    private String statut;
    private Boolean actif;
    private LocalDate dateEmbauche;
    private LocalDate dateCreation;
    private Integer soldeConges;
    private String role;

    // ── Champs spécifiques AdministrateurRH ────────
    // (AdministrateurRH n'a pas de champs supplémentaires dans votre code actuel)

    // ── Relation Employe (optionnelle) ────────────
    // Un administrateur RH peut être associé à un employé (mais ce n'est pas obligatoire)
    private Long employeId;
    private String employeMatricule;
    private String employeNom;
    private String employePrenom;

    // ── Métadonnées ───────────────────────────────
    private String nomComplet;
    private String statutCompte;
    private boolean peutSeConnecter;
    private Long anciennete;
}