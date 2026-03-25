package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entité AdministrateurRH - Administrateur du système RH
 * Corrigée pour correspondre au diagramme de classes :
 * - Hérite désormais de Utilisateur (flèche d'héritage dans le diagramme)
 * - Stratégie JOINED : la table "administrateurs_rh" ne stocke que
 *   la PK + les colonnes spécifiques ; les colonnes communes sont dans "utilisateurs"
 */
@Entity
@Table(name = "administrateurs_rh")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@DiscriminatorValue("ADMIN_RH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdministrateurRH extends Utilisateur {

    /* ── Relation avec Employe (1-1) ────────────────── */

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", unique = true)
    @ToString.Exclude
    private Employe employe;

    /* ── Méthodes métier - Gestion des Employés ─────── */

    public void creerEmploye()    { /* délégué au service */ }
    public void modifierEmploye() { /* délégué au service */ }
    public void supprimerEmploye(){ /* délégué au service */ }
    public void consulterEmploye(){ /* délégué au service */ }

    /* ── Méthodes métier - Gestion des Compétences ──── */

    public void creerCompetence()             { /* délégué au service */ }
    public void modifierCompetence()          { /* délégué au service */ }
    public void supprimerCompetence()         { /* délégué au service */ }
    public void associerCompetenceEmploye()   { /* délégué au service */ }

    /* ── Méthodes métier - Gestion des Formations ───── */

    public void creerFormation()   { /* délégué au service */ }
    public void modifierFormation(){ /* délégué au service */ }
    public void supprimerFormation(){ /* délégué au service */ }

    /* ── Méthodes métier - Reporting ────────────────── */

    public void consulterDashboardsRH() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif");
    }

    public void analyserTurnover()     { /* délégué au SystemeBI */ }
    public void analyserAbsenteisme()  { /* délégué au SystemeBI */ }
    public void consulterScoresRisque(){ /* délégué au SystemeBI */ }
}