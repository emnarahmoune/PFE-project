package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Table(name = "formation_video")
public class FormationVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    private String urlYoutube;

    private int ordre;

    @ManyToOne
    @JoinColumn(name = "formation_id")
    private Formation formation;

    // GETTERS
    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public String getUrlYoutube() { return urlYoutube; }
    public int getOrdre() { return ordre; }
    public Formation getFormation() { return formation; }

    // SETTERS
    public void setFormation(Formation formation) { this.formation = formation; }
}