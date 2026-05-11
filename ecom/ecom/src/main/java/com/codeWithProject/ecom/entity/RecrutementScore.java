package com.codeWithProject.ecom.entity;


import com.codeWithProject.ecom.entity.enums.NiveauCompatibilite;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recrutement_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecrutementScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false, unique = true)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreRecrutement offre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    private Integer scoreGlobal;

    private Integer scoreCompetences;

    private Integer scoreTechnologies;

    private Integer scoreExperience;

    private Integer scoreFormation;

    @Enumerated(EnumType.STRING)
    private NiveauCompatibilite niveauCompatibilite;

    @ElementCollection
    @CollectionTable(
            name = "score_competences_correspondantes",
            joinColumns = @JoinColumn(name = "score_id")
    )
    @Column(name = "competence")
    @Builder.Default
    private List<String> competencesCorrespondantes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "score_competences_manquantes",
            joinColumns = @JoinColumn(name = "score_id")
    )
    @Column(name = "competence")
    @Builder.Default
    private List<String> competencesManquantes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "score_technologies_correspondantes",
            joinColumns = @JoinColumn(name = "score_id")
    )
    @Column(name = "technologie")
    @Builder.Default
    private List<String> technologiesCorrespondantes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "score_technologies_manquantes",
            joinColumns = @JoinColumn(name = "score_id")
    )
    @Column(name = "technologie")
    @Builder.Default
    private List<String> technologiesManquantes = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String justificationIa;

    @Column(columnDefinition = "TEXT")
    private String recommandationIa;

    private LocalDateTime dateCalcul;

    @PrePersist
    public void prePersist() {
        if (dateCalcul == null) {
            dateCalcul = LocalDateTime.now();
        }
    }
}