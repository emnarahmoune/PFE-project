package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "evaluations", indexes = {
        @Index(name = "idx_eval_employe", columnList = "employe_id"),
        @Index(name = "idx_eval_date", columnList = "date_evaluation")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    @Column(nullable = false)
    private Double note; // Note sur 10

    @Column(name = "objectifs_atteints")
    private Integer objectifsAtteints; // Pourcentage 0-100

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id")
    private Employe evaluateur; // Manager ou RH qui évalue
}