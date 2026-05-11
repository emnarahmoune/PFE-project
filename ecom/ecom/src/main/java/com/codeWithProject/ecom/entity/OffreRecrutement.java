package com.codeWithProject.ecom.entity;


import com.codeWithProject.ecom.entity.enums.StatutOffreRecrutement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offres_recrutement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffreRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titrePoste;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String departement;

    private String typeContrat;

    private String localisation;

    private Integer experienceMin;

    private String niveauEtude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutOffreRecrutement statut = StatutOffreRecrutement.BROUILLON;

    private LocalDateTime datePublication;

    private LocalDate dateExpiration;

    private Long creeParId;

@Column(precision = 10, scale = 2)
private BigDecimal salairePropose;

    private String creeParNom;

    @ElementCollection
    @CollectionTable(
            name = "offre_recrutement_competences",
            joinColumns = @JoinColumn(name = "offre_id")
    )
    @Column(name = "competence")
    @Builder.Default
    private List<String> competencesRequises = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "offre_recrutement_technologies",
            joinColumns = @JoinColumn(name = "offre_id")
    )
    @Column(name = "technologie")
    @Builder.Default
    private List<String> technologiesRequises = new ArrayList<>();

    @OneToMany(mappedBy = "offre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Candidature> candidatures = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (statut == null) {
            statut = StatutOffreRecrutement.BROUILLON;
        }

        if (statut == StatutOffreRecrutement.OUVERTE && datePublication == null) {
            datePublication = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (statut == StatutOffreRecrutement.OUVERTE && datePublication == null) {
            datePublication = LocalDateTime.now();
        }
    }
}