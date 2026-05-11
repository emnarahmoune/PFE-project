package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poste_competence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosteCompetence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competence_id", nullable = false)
    private Competence competence;

    @Column(name = "niveau_requis", nullable = false)
    private Integer niveauRequis;
}