package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    // =========================
    // 🎥 VIDEOS
    // =========================
    @OneToMany(
        mappedBy = "formation",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JsonIgnore
    private Set<FormationVideo> videos = new HashSet<>();

    // =========================
    // 📄 SUPPORTS
    // =========================
    @OneToMany(
        mappedBy = "formation",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JsonIgnore
    private Set<FormationSupport> supports = new HashSet<>();

    // =========================
    // 👥 EMPLOYES
    // =========================
    @ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "employe_formation",
    joinColumns = @JoinColumn(name = "formation_id"),
    inverseJoinColumns = @JoinColumn(name = "employe_id")
)
@JsonIgnore
private Set<Employe> employes = new HashSet<>();


    public void setEmployes(Set<Employe> employes) {
        this.employes = employes;
    }
}