package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "indicateurs_rh", indexes = {
        @Index(name = "idx_irh_type", columnList = "type"),
        @Index(name = "idx_irh_periode", columnList = "periode"),
        @Index(name = "idx_irh_employe", columnList = "employe_id")
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
    private Double valeur;

    @Column(name = "date_calcul", nullable = false)
    private LocalDate dateCalcul;

    @Column(length = 50)
    private String periode; // MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "mois")
    private Integer mois;

    @Column(name = "trimestre")
    private Integer trimestre;

    @Column(length = 100)
    private String departement;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "tendance", length = 20)
    private String tendance;

    @Column(name = "valeur_precedente")
    private Double valeurPrecedente;

    // 🔥 NOUVEAU : lien vers l'employé (nullable car indicateur global)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id")
    @ToString.Exclude
    private Employe employe;

    // Relation avec SystemeBI (inchangée)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "systeme_bi_id")
    @ToString.Exclude
    private SystemeBI systemeBI;

    // Méthodes métier simplifiées (on ne garde que la logique utile)
    public void calculerIndicateur() {
        this.dateCalcul = LocalDate.now();
        this.annee = this.dateCalcul.getYear();
        this.mois = this.dateCalcul.getMonthValue();
        this.trimestre = (this.mois - 1) / 3 + 1;
    }

    public void determinerTendance() {
        if (this.valeurPrecedente == null) {
            this.tendance = "STABLE";
            return;
        }
        double variation = ((this.valeur - this.valeurPrecedente) / this.valeurPrecedente) * 100;
        if (variation > 5) this.tendance = "HAUSSE";
        else if (variation < -5) this.tendance = "BAISSE";
        else this.tendance = "STABLE";
    }

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