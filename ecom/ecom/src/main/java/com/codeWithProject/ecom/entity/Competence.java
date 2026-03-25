package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Competence - Catalogue des compétences
 * Conforme au diagramme de classes :
 *  - id          : Long
 *  - nom         : String
 *  - description : String
 *  - categorie   : String   (TECHNIQUE, SOFT_SKILL, LINGUISTIQUE…)
 */
@Entity
@Table(name = "competences",
        indexes = {
                @Index(name = "idx_competence_nom",       columnList = "nom"),
                @Index(name = "idx_competence_categorie", columnList = "categorie")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competence {

    /* ── Attributs du diagramme ──────────────────────────── */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 150)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** TECHNIQUE, SOFT_SKILL, LINGUISTIQUE, METHODOLOGIQUE… */
    @Column(name = "categorie", length = 50)
    private String categorie;

    /* ── Relations ───────────────────────────────────────── */

    @OneToMany(mappedBy = "competence", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<EmployeCompetence> employeCompetences = new ArrayList<>();

    /* ── Méthodes métier du diagramme ────────────────────── */

    public void creer()    { /* délégué au service */ }
    public void modifier() { /* délégué au service */ }
    public void supprimer(){ /* délégué au service */ }
    public void consulter(){ /* délégué au service */ }

    /* ── Lifecycle ───────────────────────────────────────── */

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (this.nom       != null) this.nom       = this.nom.trim();
        if (this.categorie != null) this.categorie = this.categorie.trim().toUpperCase();
        if (this.nom == null || this.nom.isBlank())
            throw new IllegalStateException("Le nom de la compétence est obligatoire");
    }
}