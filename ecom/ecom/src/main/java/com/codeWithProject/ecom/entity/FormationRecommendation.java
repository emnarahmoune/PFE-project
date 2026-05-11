package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeId;

    private Long offreId;

    private String poste;

    private String type;

    private String formationTitle;

    private String provider;

    @Column(length = 1000)
    private String url;

    @Column(length = 3000)
    private String description;

    private Double score;

    private Double semanticScore;

    private Double skillScore;

    @Column(length = 1000)
    private String matchedSkills;

    @Column(length = 3000)
    private String reason;


    private Long formationId;
    
    @Column(columnDefinition = "TEXT")
private String videosJson;
    private LocalDateTime dateCreation;
}