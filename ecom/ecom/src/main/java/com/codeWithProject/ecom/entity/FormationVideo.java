package com.codeWithProject.ecom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formation_video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(name = "url_youtube")
    private String urlYoutube;

    private int ordre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "formation_id")
    @JsonIgnore
    private Formation formation;
}