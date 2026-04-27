// ScoreTurnover.java (entité allégée)
package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "scores_turnover")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreTurnover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double score; // 0-100
    private String niveauRisque;
    private LocalDate datePrediction;
    private String periodePrediction;
    private String facteursPrincipaux;
    private Double confianceModele;
    private String versionModele;
    private String actionRecommandee;

    // Scores individuels
    private Double scoreAnciennete;
    private Double scoreSalaire;
    private Double scorePerformance;
    private Double scoreFormation;
    private Double scoreAbsenteisme;

    @ManyToOne
    @JoinColumn(name = "employe_id")
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "systeme_bi_id")
    private SystemeBI systemeBI;

    // Plus de logique métier ici, tout sera fait dans le service
}