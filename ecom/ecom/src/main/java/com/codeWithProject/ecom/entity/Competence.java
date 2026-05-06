package com.codeWithProject.ecom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "competences",
        indexes = {
                @Index(name = "idx_competence_nom", columnList = "nom"),
                @Index(name = "idx_competence_categorie", columnList = "categorie")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Competence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 150)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "categorie", length = 50)
    private String categorie;

    @JsonIgnore
    @OneToMany(mappedBy = "competence", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<EmployeCompetence> employeCompetences = new ArrayList<>();

    /*
     * Garde cette relation seulement si l'entité Employe contient bien :
     *
     * @ManyToMany
     * private List<Competence> competences;
     *
     * Si Employe ne contient pas ce champ, supprime ce bloc.
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "competences")
    @ToString.Exclude
    @Builder.Default
    private List<Employe> employes = new ArrayList<>();

    public void creer() {
        // délégué au service
    }

    public void modifier() {
        // délégué au service
    }

    public void supprimer() {
        // délégué au service
    }

    public void consulter() {
        // délégué au service
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (this.nom != null) {
            this.nom = this.nom.trim();
        }

        if (this.categorie != null) {
            this.categorie = this.categorie.trim().toUpperCase();
        }

        if (this.nom == null || this.nom.isBlank()) {
            throw new IllegalStateException("Le nom de la compétence est obligatoire");
        }
    }
}