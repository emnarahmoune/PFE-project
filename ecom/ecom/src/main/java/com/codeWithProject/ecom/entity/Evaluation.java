package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "evaluations", indexes = {
        @Index(name = "idx_eval_employe", columnList = "employe_id"),
        @Index(name = "idx_eval_evaluateur", columnList = "evaluateur_id"),
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

    /**
     * Employé évalué.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    /**
     * Note sur 10.
     */
    @Column(nullable = false)
    private Double note;

    /**
     * Pourcentage 0-100.
     * Utilisé par les statistiques objectifs.
     */
    @Column(name = "objectifs_atteints")
    private Integer objectifsAtteints;

    /**
     * Ancien commentaire global / legacy.
     */
    @Column(length = 2000)
    private String commentaire;

    /**
     * Nouveaux champs utilisés par le front évaluations.
     */
    @Column(name = "points_forts", length = 2000)
    private String pointsForts;

    @Column(name = "axes_amelioration", length = 2000)
    private String axesAmelioration;

    @Column(name = "commentaire_manager", length = 2000)
    private String commentaireManager;

    @Column(name = "objectifs", length = 2000)
    private String objectifs;

    /**
     * Manager ou RH qui a réalisé l'évaluation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id")
    private Employe evaluateur;


    @Column(name = "periode")
private String periode;



@Column(name = "note_technique")
private Double noteTechnique;

@Column(name = "note_communication")
private Double noteCommunication;

@Column(name = "note_leadership")
private Double noteLeadership;

@Column(name = "note_ponctualite")
private Double notePonctualite;

@Column(name = "note_productivite")
private Double noteProductivite;


@Column(name = "statut", length = 30, nullable = false)
@Builder.Default
private String statut = "PUBLIEE";



@PrePersist
@PreUpdate
private void normalizeStatut() {
    if (statut == null || statut.isBlank()) {
        statut = "PUBLIEE";
    }

    statut = statut.trim().toUpperCase();

    if (!statut.equals("BROUILLON")
            && !statut.equals("PUBLIEE")
            && !statut.equals("VALIDEE")
            && !statut.equals("ARCHIVEE")) {
        statut = "PUBLIEE";
    }
}
}