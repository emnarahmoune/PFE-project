package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;

@Entity
public class FormationCompetence {

    @Id
    @GeneratedValue
    private Long id;

    private Long formationId;

    @ManyToOne
    private Competence competence;

    // getters
    public Long getId() { return id; }

    public Long getFormationId() { return formationId; }

    public Competence getCompetence() { return competence; }

    // setters
    public void setId(Long id) { this.id = id; }

    public void setFormationId(Long formationId) { this.formationId = formationId; }

    public void setCompetence(Competence competence) { this.competence = competence; }
}