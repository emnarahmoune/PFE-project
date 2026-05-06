package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "formation_competence")
@Getter
@Setter
public class FormationCompetence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "formation_id", nullable = false)
    private Long formationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competence_id", nullable = false)
    private Competence competence;
}