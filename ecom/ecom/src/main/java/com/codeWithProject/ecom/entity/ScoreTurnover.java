package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Entité ScoreTurnover - Représente un score prédictif de risque de départ d'un employé
 */
@Entity
@Table(name = "scores_turnover", indexes = {
        @Index(name = "idx_st_employe", columnList = "employe_id"),
        @Index(name = "idx_st_niveau_risque", columnList = "niveau_risque"),
        @Index(name = "idx_st_date", columnList = "date_prediction"),
        @Index(name = "idx_st_score", columnList = "score")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreTurnover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double score; // Score de 0 à 100

    @Column(name = "niveau_risque", length = 50, nullable = false)
    private String niveauRisque; // FAIBLE, MOYEN, ELEVE, CRITIQUE

    @Column(name = "date_prediction", nullable = false)
    private LocalDate datePrediction;

    @Column(name = "periode_prediction", length = 50)
    @Builder.Default
    private String periodePrediction = "6_MOIS";

    @Column(columnDefinition = "TEXT")
    private String facteursPrincipaux;

    @Column(name = "confiance_modele")
    private Double confianceModele;

    @Column(name = "version_modele", length = 50)
    private String versionModele;

    // Features individuelles
    @Column(name = "score_anciennete")
    private Double scoreAnciennete;

    @Column(name = "score_salaire")
    private Double scoreSalaire;

    @Column(name = "score_performance")
    private Double scorePerformance;

    @Column(name = "score_formation")
    private Double scoreFormation;

    @Column(name = "score_absenteisme")
    private Double scoreAbsenteisme;

    @Column(name = "action_recommandee", columnDefinition = "TEXT")
    private String actionRecommandee;

    // ===== RELATIONS =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "systeme_bi_id")
    @ToString.Exclude
    private SystemeBI systemeBI;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id")
    @ToString.Exclude
    private Employe employe;

    // ===== CONSTANTES =====
    private static final double POIDS_ANCIENNETE = 0.25;
    private static final double POIDS_SALAIRE = 0.30;
    private static final double POIDS_PERFORMANCE = 0.20;
    private static final double POIDS_FORMATION = 0.15;
    private static final double POIDS_ABSENTEISME = 0.10;

    // ===== MÉTHODES MÉTIER =====

    public void calculerScore() {
        if (this.employe == null) {
            throw new IllegalStateException("Impossible de calculer le score sans employé");
        }

        this.datePrediction = LocalDate.now();

        // Initialiser les scores avec des valeurs par défaut
        this.scoreAnciennete = 0.0;
        this.scoreSalaire = 0.0;
        this.scorePerformance = 10.0; // Valeur par défaut
        this.scoreFormation = 0.0;
        this.scoreAbsenteisme = 5.0; // Valeur par défaut

        calculerScoreAnciennete();
        calculerScoreSalaire();
        calculerScoreFormation();

        this.score = calculerScoreGlobal();
        determinerNiveauRisque();
        identifierFacteursPrincipaux();
        recommanderActions();
    }

    private void calculerScoreAnciennete() {
        if (this.employe != null && this.employe.getDateEmbauche() != null) {
            long anciennete = calculerAnciennete();

            if (anciennete < 1) {
                this.scoreAnciennete = 30.0;
            } else if (anciennete >= 1 && anciennete < 3) {
                this.scoreAnciennete = 15.0;
            } else if (anciennete >= 3 && anciennete < 5) {
                this.scoreAnciennete = 5.0;
            } else {
                this.scoreAnciennete = 25.0;
            }
        }
    }

    private void calculerScoreSalaire() {
        if (this.employe != null && this.employe.getSalaire() != null) {
            Double salaire = this.employe.getSalaire();

            if (salaire < 30000) {
                this.scoreSalaire = 30.0;
            } else if (salaire < 45000) {
                this.scoreSalaire = 15.0;
            } else if (salaire < 60000) {
                this.scoreSalaire = 5.0;
            } else {
                this.scoreSalaire = 0.0;
            }
        }
    }

    private void calculerScoreFormation() {
        if (this.employe != null) {
            int nombreFormations = this.employe.getFormations() != null ?
                    this.employe.getFormations().size() : 0;

            if (nombreFormations == 0) {
                this.scoreFormation = 20.0;
            } else if (nombreFormations < 3) {
                this.scoreFormation = 10.0;
            } else {
                this.scoreFormation = 0.0;
            }
        }
    }

    private Double calculerScoreGlobal() {
        return (this.scoreAnciennete * POIDS_ANCIENNETE) +
                (this.scoreSalaire * POIDS_SALAIRE) +
                (this.scorePerformance * POIDS_PERFORMANCE) +
                (this.scoreFormation * POIDS_FORMATION) +
                (this.scoreAbsenteisme * POIDS_ABSENTEISME);
    }

    private void determinerNiveauRisque() {
        if (this.score < 20) {
            this.niveauRisque = "FAIBLE";
        } else if (this.score < 40) {
            this.niveauRisque = "MOYEN";
        } else if (this.score < 70) {
            this.niveauRisque = "ELEVE";
        } else {
            this.niveauRisque = "CRITIQUE";
        }
    }

    private void identifierFacteursPrincipaux() {
        StringBuilder facteurs = new StringBuilder();

        if (this.scoreAnciennete >= 20) {
            facteurs.append("Ancienneté critique; ");
        }
        if (this.scoreSalaire >= 20) {
            facteurs.append("Salaire bas; ");
        }
        if (this.scorePerformance >= 20) {
            facteurs.append("Problèmes de performance; ");
        }
        if (this.scoreFormation >= 15) {
            facteurs.append("Manque de formations; ");
        }
        if (this.scoreAbsenteisme >= 15) {
            facteurs.append("Absentéisme élevé; ");
        }

        this.facteursPrincipaux = facteurs.length() > 0 ?
                facteurs.toString() : "Aucun facteur critique identifié";
    }

    private void recommanderActions() {
        StringBuilder actions = new StringBuilder();

        if (this.scoreSalaire >= 20) {
            actions.append("- Revoir la rémunération\n");
        }
        if (this.scoreFormation >= 15) {
            actions.append("- Proposer des formations adaptées\n");
        }
        if (this.scoreAnciennete >= 20 && this.employe != null && this.employe.getDateEmbauche() != null && calculerAnciennete() > 5) {
            actions.append("- Discuter des opportunités d'évolution\n");
        }
        if (this.scoreAnciennete >= 20 && this.employe != null && this.employe.getDateEmbauche() != null && calculerAnciennete() < 1) {
            actions.append("- Renforcer l'onboarding et le suivi\n");
        }
        if (this.scoreAbsenteisme >= 15) {
            actions.append("- Entretien RH pour comprendre les causes d'absence\n");
        }

        if ("CRITIQUE".equals(this.niveauRisque) || "ELEVE".equals(this.niveauRisque)) {
            actions.append("- Planifier un entretien individuel urgent\n");
        }

        this.actionRecommandee = actions.length() > 0 ?
                actions.toString() : "Maintenir le suivi régulier";
    }

    /**
     * Calcule l'ancienneté en années
     */
    private long calculerAnciennete() {
        if (this.employe == null || this.employe.getDateEmbauche() == null) {
            return 0;
        }
        return ChronoUnit.YEARS.between(this.employe.getDateEmbauche(), LocalDate.now());
    }

    public void predireRisque() {
        calculerScore();
        this.confianceModele = 0.85;
        this.versionModele = "v1.0.0";
    }

    @Transient
    public boolean necessiteAlerte() {
        return "ELEVE".equals(this.niveauRisque) || "CRITIQUE".equals(this.niveauRisque);
    }

    @Transient
    public String getCouleurAffichage() {
        switch (this.niveauRisque) {
            case "FAIBLE": return "#28a745";
            case "MOYEN": return "#ffc107";
            case "ELEVE": return "#fd7e14";
            case "CRITIQUE": return "#dc3545";
            default: return "#6c757d";
        }
    }

    @Transient
    public String getResume() {
        return String.format(
                "Score: %.1f%% - Niveau: %s - Confiance: %.0f%%",
                this.score != null ? this.score : 0,
                this.niveauRisque != null ? this.niveauRisque : "N/A",
                this.confianceModele != null ? this.confianceModele * 100 : 0
        );
    }

    @Transient
    public boolean isValide() {
        if (this.datePrediction == null) {
            return false;
        }
        long joursDepuisPrediction = ChronoUnit.DAYS.between(
                this.datePrediction,
                LocalDate.now()
        );
        return joursDepuisPrediction < 30;
    }

    @PrePersist
    protected void onCreate() {
        if (this.datePrediction == null) {
            this.datePrediction = LocalDate.now();
        }
        if (this.periodePrediction == null) {
            this.periodePrediction = "6_MOIS";
        }
        if (this.confianceModele == null) {
            this.confianceModele = 0.85;
        }
        if (this.versionModele == null) {
            this.versionModele = "v1.0.0";
        }
    }
}