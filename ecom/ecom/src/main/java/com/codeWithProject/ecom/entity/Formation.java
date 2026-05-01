package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Entity
@Table(name = "formations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String domaine;

    @Column(name = "duree_heures")
    private Integer dureeHeures;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

 
    @Column(name = "url_youtube")
    private String urlVideo;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();

        if (this.actif == null) {
            this.actif = true;
        }
    }


    @OneToMany(
        mappedBy = "formation",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
private List<FormationVideo> videos = new ArrayList<>();

@OneToMany(
        mappedBy = "formation",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
private List<FormationSupport> supports = new ArrayList<>();

@ManyToMany
private List<Employe> employes;

public List<Employe> getEmployes() {
    return employes;
}

public void setEmployes(List<Employe> employes) {
    this.employes = employes;
}
}