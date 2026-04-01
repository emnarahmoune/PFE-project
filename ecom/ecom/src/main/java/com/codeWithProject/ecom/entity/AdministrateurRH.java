package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entité AdministrateurRH - Administrateur du système RH
 * Corrigée pour correspondre au diagramme de classes :
 * - Hérite de Utilisateur (flèche d'héritage dans le diagramme)
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

    /* ── Méthodes statiques de création ────────────────── */

    /**
     * Crée un administrateur RH à partir d'un employé existant
     * Cette méthode doit être utilisée lors de la promotion d'un employé en administrateur
     *
     * @param employe L'employé à promouvoir
     * @param password Le mot de passe pour le compte administrateur
     * @return Un nouvel administrateur RH avec les informations de l'employé
     */
    public static AdministrateurRH fromEmploye(Employe employe, String password) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        AdministrateurRH admin = AdministrateurRH.builder()
                .nom(employe.getNom())
                .prenom(employe.getPrenom())
                .email(employe.getEmail())
                .telephone(employe.getTelephone())
                .password(password)
                .actif(true)
                .compteVerrouille(false)
                .nombreConnexions(0)
                .tentativesEchec(0)
                .employe(employe)
                .build();

        return admin;
    }

    /**
     * Crée un administrateur RH avec des informations personnalisées
     *
     * @param nom Nom de l'administrateur
     * @param prenom Prénom de l'administrateur
     * @param email Email de l'administrateur
     * @param password Mot de passe
     * @return Un nouvel administrateur RH
     */
    public static AdministrateurRH create(String nom, String prenom, String email, String password) {
        return AdministrateurRH.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .password(password)
                .actif(true)
                .compteVerrouille(false)
                .nombreConnexions(0)
                .tentativesEchec(0)
                .build();
    }

    /* ── Méthodes métier - Gestion des Employés ─────── */

    /**
     * Crée un nouvel employé avec son compte utilisateur associé
     * Cette méthode est appelée par le service
     */
    public void creerEmploye(Employe employe, String password) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        // S'assurer que l'employé a un utilisateur associé
        if (employe.getUtilisateur() == null) {
            employe.createUtilisateur(password);
        }

        // Logique déléguée au service
    }

    /**
     * Modifie un employé existant
     */
    public void modifierEmploye(Employe employe) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        // Synchroniser les informations avec l'utilisateur associé
        if (employe.getUtilisateur() != null) {
            employe.getUtilisateur().setNom(employe.getNom());
            employe.getUtilisateur().setPrenom(employe.getPrenom());
            employe.getUtilisateur().setEmail(employe.getEmail());
            employe.getUtilisateur().setTelephone(employe.getTelephone());
        }

        // Logique déléguée au service
    }

    /**
     * Supprime un employé
     */
    public void supprimerEmploye(Employe employe) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        // Logique déléguée au service
    }

    /**
     * Consulte les informations d'un employé
     */
    public void consulterEmploye(Employe employe) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        // Vérifier que l'administrateur est actif
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de consulter les employés");
        }

        // Logique déléguée au service
    }

    /* ── Méthodes métier - Gestion des Compétences ──── */

    /**
     * Crée une nouvelle compétence
     */
    public void creerCompetence(Competence competence) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de créer une compétence");
        }
        // Logique déléguée au service
    }

    /**
     * Modifie une compétence existante
     */
    public void modifierCompetence(Competence competence) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de modifier une compétence");
        }
        // Logique déléguée au service
    }

    /**
     * Supprime une compétence
     */
    public void supprimerCompetence(Competence competence) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de supprimer une compétence");
        }
        // Logique déléguée au service
    }

    /**
     * Associe une compétence à un employé
     */
    public void associerCompetenceEmploye(Employe employe, Competence competence, String niveau) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible d'associer une compétence");
        }
        // Logique déléguée au service
    }

    /* ── Méthodes métier - Gestion des Formations ───── */

    /**
     * Crée une nouvelle formation
     */
    public void creerFormation(Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de créer une formation");
        }
        // Logique déléguée au service
    }

    /**
     * Modifie une formation existante
     */
    public void modifierFormation(Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de modifier une formation");
        }
        // Logique déléguée au service
    }

    /**
     * Supprime une formation
     */
    public void supprimerFormation(Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de supprimer une formation");
        }
        // Logique déléguée au service
    }

    /**
     * Inscrit un employé à une formation
     */
    public void inscrireEmployeFormation(Employe employe, Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible d'inscrire à une formation");
        }
        // Logique déléguée au service
    }

    /* ── Méthodes métier - Reporting ────────────────── */

    /**
     * Consulte les tableaux de bord RH
     */
    public void consulterDashboardsRH() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de consulter les tableaux de bord");
        }
        // Logique déléguée au service
    }

    /**
     * Analyse le turnover de l'entreprise
     */
    public void analyserTurnover() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible d'analyser le turnover");
        }
        // Logique déléguée au SystemeBI
    }

    /**
     * Analyse l'absentéisme
     */
    public void analyserAbsenteisme() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible d'analyser l'absentéisme");
        }
        // Logique déléguée au SystemeBI
    }

    /**
     * Consulte les scores de risque
     */
    public void consulterScoresRisque() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de consulter les scores de risque");
        }
        // Logique déléguée au SystemeBI
    }

    /**
     * Génère un rapport sur les employés
     */
    public void genererRapportEmployes() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de générer un rapport");
        }
        // Logique déléguée au service
    }

    /**
     * Génère un rapport sur les formations
     */
    public void genererRapportFormations() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible de générer un rapport");
        }
        // Logique déléguée au service
    }

    /**
     * Exporte les données RH
     */
    public void exporterDonneesRH() {
        if (!Boolean.TRUE.equals(this.getActif())) {
            throw new IllegalStateException("Administrateur inactif - Impossible d'exporter les données");
        }
        // Logique déléguée au service
    }

    /* ── Méthodes utilitaires ────────────────────────── */

    /**
     * Vérifie si l'administrateur est actif
     */
    @Transient
    public boolean isActif() {
        return Boolean.TRUE.equals(this.getActif());
    }

    /**
     * Vérifie si l'administrateur peut effectuer des opérations
     */
    @Transient
    public boolean peutEffectuerOperations() {
        return isActif() && !Boolean.TRUE.equals(this.getCompteVerrouille());
    }

    /**
     * Retourne le nom complet de l'administrateur
     */
    @Transient
    @Override
    public String getNomComplet() {
        return super.getNomComplet() + " (Administrateur RH)";
    }

    /**
     * Retourne une représentation textuelle de l'administrateur
     */
    @Override
    public String toString() {
        return String.format("AdministrateurRH{id=%d, nom='%s', prenom='%s', email='%s', actif=%s, employeId=%s}",
                this.getId(), this.getNom(), this.getPrenom(), this.getEmail(),
                this.getActif(), this.employe != null ? this.employe.getId() : "null");
    }

    /* ── Lifecycle ──────────────────────────────────── */

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        super.onPrePersistOrUpdate();

        // S'assurer que le type est correct
        if (this.getTypeUtilisateur() == null) {
            // Le type sera automatiquement défini par la stratégie d'héritage
        }

        // Validation supplémentaire pour les administrateurs
        if (this.getPassword() == null || this.getPassword().isEmpty()) {
            throw new IllegalStateException("Le mot de passe de l'administrateur est obligatoire");
        }

        // S'assurer que l'employé associé (si présent) a les mêmes informations
        if (this.employe != null) {
            if (!this.getNom().equals(this.employe.getNom())) {
                this.employe.setNom(this.getNom());
            }
            if (!this.getPrenom().equals(this.employe.getPrenom())) {
                this.employe.setPrenom(this.getPrenom());
            }
            if (!this.getEmail().equals(this.employe.getEmail())) {
                this.employe.setEmail(this.getEmail());
            }
            if (this.getTelephone() != null && !this.getTelephone().equals(this.employe.getTelephone())) {
                this.employe.setTelephone(this.getTelephone());
            }
        }
    }
}