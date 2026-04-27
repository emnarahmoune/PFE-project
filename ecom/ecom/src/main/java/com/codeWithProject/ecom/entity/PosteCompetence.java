package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Entity
public class PosteCompetence {

    @Id
    @GeneratedValue
    private Long id;

    private String poste;

    @ManyToOne
    private Competence competence;
    
    private Integer niveauRequis;
    public Competence getCompetence() {
        return competence;
    }

    public int getNiveauRequis() {
        return niveauRequis;
    }

    public String getPoste() {
        return poste;
    }

    public Long getId() {
        return id;
    }

}