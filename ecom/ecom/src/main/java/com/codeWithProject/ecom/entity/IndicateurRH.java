package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entité IndicateurRH - Représente un indicateur RH calculé par le système BI
 *
 * Types d'indicateurs:
 * - TURNOVER: Taux de rotation du personnel
 * - ABSENTEISME: Taux d'absence
 * - PERFORMANCE: Indicateur de performance global
 * - SATISFACTION: Taux de satisfaction des employés
 * - COMPETENCES: Couverture des compétences clés
 *
 * Relations:
 * - ManyToOne avec SystemeBI
 */
@Entity
@Table(name = "indicateurs_rh", indexes = {
        @Index(name = "idx_irh_type", columnList = "type"),
        @Index(name = "idx_irh_periode", columnList = "periode"),
        @Index(name = "idx_irh_date", columnList = "date_calcul")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicateurRH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // TURNOVER, ABSENTEISME, PERFORMANCE, SATISFACTION, COMPETENCES

    @Column(nullable = false)
    private Double valeur; // Valeur de l'indicateur (pourcentage ou nombre)

    @Column(name = "date_calcul", nullable = false)
    private LocalDate dateCalcul;

    @Column(length = 50)
    private String periode; // MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "mois")
    private Integer mois; // 1-12 pour les indicateurs mensuels

    @Column(name = "trimestre")
    private Integer trimestre; // 1-4 pour les indicateurs trimestriels

    @Column(length = 100)
    private String departement; // Si l'indicateur est spécifique à un département

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "tendance", length = 20)
    private String tendance; // HAUSSE, BAISSE, STABLE

    @Column(name = "valeur_precedente")
    private Double valeurPrecedente; // Pour comparer avec la période précédente

    // ===== RELATIONS =====

    /**
     * Relation ManyToOne avec SystemeBI
     * Chaque indicateur est calculé par un système BI
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "systeme_bi_id")
    @ToString.Exclude
    private SystemeBI systemeBI;

    // ===== MÉTHODES MÉTIER =====

    /**
     * Charge les données RH nécessaires au calcul
     */
    public void chargerDonneesRH() {
        // Logique de chargement des données depuis la base
        // Sera implémentée dans le service
    }

    /**
     * Calcule la valeur de l'indicateur
     */
    public void calculerIndicateur() {
        this.dateCalcul = LocalDate.now();
        this.annee = this.dateCalcul.getYear();
        this.mois = this.dateCalcul.getMonthValue();
        this.trimestre = (this.mois - 1) / 3 + 1;

        // La logique de calcul spécifique dépend du type d'indicateur
        switch (this.type) {
            case "TURNOVER":
                calculerTurnover();
                break;
            case "ABSENTEISME":
                calculerAbsenteisme();
                break;
            case "PERFORMANCE":
                calculerPerformance();
                break;
            case "SATISFACTION":
                calculerSatisfaction();
                break;
            case "COMPETENCES":
                calculerCouvertureCompetences();
                break;
            default:
                throw new IllegalArgumentException("Type d'indicateur non supporté: " + this.type);
        }

        // Déterminer la tendance
        determinerTendance();
    }

    /**
     * Calcule le taux de turnover
     */
    private void calculerTurnover() {
        // Logique de calcul du turnover:
        // Taux = (Nombre de départs / Effectif moyen) * 100

        // À implémenter avec requêtes base de données
        // this.valeur = ...;
    }

    /**
     * Calcule le taux d'absentéisme
     */
    private void calculerAbsenteisme() {
        // Logique de calcul de l'absentéisme:
        // Taux = (Jours d'absence / Jours travaillés théoriques) * 100

        // À implémenter avec requêtes base de données
        // this.valeur = ...;
    }

    /**
     * Calcule l'indicateur de performance
     */
    private void calculerPerformance() {
        // Logique de calcul de la performance globale
        // Basé sur les évaluations, objectifs atteints, etc.

        // À implémenter
        // this.valeur = ...;
    }

    /**
     * Calcule le taux de satisfaction
     */
    private void calculerSatisfaction() {
        // Logique de calcul de la satisfaction
        // Basé sur des enquêtes, feedbacks, etc.

        // À implémenter
        // this.valeur = ...;
    }

    /**
     * Calcule la couverture des compétences clés
     */
    private void calculerCouvertureCompetences() {
        // Logique de calcul:
        // Pourcentage de compétences clés couvertes dans l'organisation

        // À implémenter
        // this.valeur = ...;
    }

    /**
     * Détermine la tendance par rapport à la période précédente
     */
    private void determinerTendance() {
        if (this.valeurPrecedente == null) {
            this.tendance = "STABLE";
            return;
        }

        double variation = ((this.valeur - this.valeurPrecedente) / this.valeurPrecedente) * 100;

        if (variation > 5) {
            this.tendance = "HAUSSE";
        } else if (variation < -5) {
            this.tendance = "BAISSE";
        } else {
            this.tendance = "STABLE";
        }
    }

    /**
     * Compare avec l'indicateur de la période précédente
     */
    public void comparerAvecPeriodePrecedente(IndicateurRH indicateurPrecedent) {
        if (indicateurPrecedent == null) {
            return;
        }

        if (!this.type.equals(indicateurPrecedent.getType())) {
            throw new IllegalArgumentException("Impossible de comparer deux indicateurs de types différents");
        }

        this.valeurPrecedente = indicateurPrecedent.getValeur();
        determinerTendance();
    }

    /**
     * Calcule la variation en pourcentage
     */
    @Transient
    public Double getVariationPourcentage() {
        if (this.valeurPrecedente == null || this.valeurPrecedente == 0) {
            return null;
        }
        return ((this.valeur - this.valeurPrecedente) / this.valeurPrecedente) * 100;
    }

    /**
     * Détermine si l'indicateur est dans la norme
     */
    @Transient
    public boolean isDansLaNorme() {
        // Définir les normes selon le type d'indicateur
        switch (this.type) {
            case "TURNOVER":
                return this.valeur < 15.0; // < 15% est acceptable
            case "ABSENTEISME":
                return this.valeur < 5.0; // < 5% est acceptable
            case "PERFORMANCE":
                return this.valeur >= 75.0; // >= 75% est bon
            case "SATISFACTION":
                return this.valeur >= 70.0; // >= 70% est bon
            case "COMPETENCES":
                return this.valeur >= 80.0; // >= 80% de couverture est bon
            default:
                return true;
        }
    }

    /**
     * Détermine le niveau d'alerte
     */
    @Transient
    public String getNiveauAlerte() {
        if (isDansLaNorme()) {
            return "VERT"; // Tout va bien
        }

        // Logique d'alerte selon le type
        switch (this.type) {
            case "TURNOVER":
            case "ABSENTEISME":
                if (this.valeur > 20.0) {
                    return "ROUGE"; // Critique
                } else if (this.valeur > 15.0) {
                    return "ORANGE"; // Attention
                }
                break;
            case "PERFORMANCE":
            case "SATISFACTION":
            case "COMPETENCES":
                if (this.valeur < 50.0) {
                    return "ROUGE"; // Critique
                } else if (this.valeur < 70.0) {
                    return "ORANGE"; // Attention
                }
                break;
        }

        return "VERT";
    }

    /**
     * Obtient une description formatée de l'indicateur
     */
    @Transient
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(this.type).append(" - ");
        desc.append(String.format("%.2f%%", this.valeur));

        if (this.periode != null) {
            desc.append(" (").append(this.periode).append(")");
        }

        if (this.tendance != null) {
            desc.append(" - Tendance: ").append(this.tendance);
        }

        if (this.departement != null) {
            desc.append(" - Département: ").append(this.departement);
        }

        return desc.toString();
    }

    /**
     * Initialisation lors de la création
     */
    @PrePersist
    protected void onCreate() {
        if (this.dateCalcul == null) {
            this.dateCalcul = LocalDate.now();
            this.annee = this.dateCalcul.getYear();
            this.mois = this.dateCalcul.getMonthValue();
            this.trimestre = (this.mois - 1) / 3 + 1;
        }
    }
}