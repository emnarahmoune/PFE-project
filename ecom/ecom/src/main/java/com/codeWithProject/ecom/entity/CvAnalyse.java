package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cv_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvAnalyse {

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

    private String cvFileName;

    private String cvContentType;

    @Column(columnDefinition = "LONGTEXT")
    private String texteExtrait;

    @ElementCollection
    @CollectionTable(
            name = "cv_analyse_competences",
            joinColumns = @JoinColumn(name = "analyse_id")
    )
    @Column(name = "competence")
    @Builder.Default
    private List<String> competencesDetectees = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "cv_analyse_technologies",
            joinColumns = @JoinColumn(name = "analyse_id")
    )
    @Column(name = "technologie")
    @Builder.Default
    private List<String> technologiesDetectees = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "cv_analyse_experiences",
            joinColumns = @JoinColumn(name = "analyse_id")
    )
    @Column(name = "experience")
    @Builder.Default
    private List<String> experiencesDetectees = new ArrayList<>();

    private Integer anneesExperienceEstimees;

    @Column(columnDefinition = "TEXT")
    private String resumeProfil;

    @ElementCollection
    @CollectionTable(
            name = "cv_analyse_points_forts",
            joinColumns = @JoinColumn(name = "analyse_id")
    )
    @Column(name = "point_fort")
    @Builder.Default
    private List<String> pointsForts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "cv_analyse_points_faibles",
            joinColumns = @JoinColumn(name = "analyse_id")
    )
    @Column(name = "point_faible")
    @Builder.Default
    private List<String> pointsFaibles = new ArrayList<>();

    private LocalDateTime dateAnalyse;

    @PrePersist
    public void prePersist() {
        if (dateAnalyse == null) {
            dateAnalyse = LocalDateTime.now();
        }
    }
}