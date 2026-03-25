package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour l'entité AdministrateurRH
 *
 * Champs directs (hérités de Utilisateur) + données de l'Employe associé.
 * Lombok @Data génère tous les getters/setters — ne pas les écrire manuellement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministrateurRHDTO {

    // ── Identifiant ────────────────────────────────
    private Long id;

    // ── Champs hérités de Utilisateur ─────────────
    private String    nom;
    private String    prenom;
    private String    email;
    private String    telephone;
    private Boolean   actif;
    private LocalDate dateCreation;

    // ── Relation Employe (optionnelle) ────────────
    private Long   employeId;
    private String matricule;   // dénormalisé pour l'affichage

}