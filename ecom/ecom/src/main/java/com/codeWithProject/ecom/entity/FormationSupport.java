package com.codeWithProject.ecom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formation_support")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(name = "fichier_url", length = 1000)
    private String fichierUrl;

    private int ordre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "formation_id")
    @JsonIgnore
    private Formation formation;
}