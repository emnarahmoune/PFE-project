package com.codeWithProject.ecom.entity;


import com.codeWithProject.ecom.entity.enums.StatutCandidature;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "candidatures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_candidature_offre_employe",
                        columnNames = {"offre_id", "employe_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreRecrutement offre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    private String cvFileName;

    private String cvOriginalName;

    private String cvContentType;

    private String cvPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCandidature statut = StatutCandidature.SOUMISE;

    private LocalDateTime dateSoumission;

    private LocalDateTime dateDecision;

    @Column(columnDefinition = "TEXT")
    private String decisionCommentaire;

    @OneToOne(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private CvAnalyse analyseCv;

    @OneToOne(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private RecrutementScore score;

    @PrePersist
    public void prePersist() {
        if (statut == null) {
            statut = StatutCandidature.SOUMISE;
        }

        if (dateSoumission == null) {
            dateSoumission = LocalDateTime.now();
        }
    }
}