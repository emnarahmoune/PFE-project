package com.codeWithProject.ecom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "employe_formation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    private Integer progression; // 0 → 100

    private String statut; // EN_COURS, TERMINE

    private LocalDateTime dateInscription;

    private LocalDateTime dateCompletion;

    private int videosCompleted=0;

    @PrePersist
    public void prePersist() {
        this.dateInscription = LocalDateTime.now();

        if (this.progression == null) {
            this.progression = 0;
        }

        if (this.statut == null) {
            this.statut = "EN_COURS";
        }
    }


@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "employe_id")
@JsonIgnore // ✅ PLUS SIMPLE ET SÛR
private Employe employe;

@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "formation_id")
@JsonIgnore // ✅ IMPORTANT
private Formation formation;
}