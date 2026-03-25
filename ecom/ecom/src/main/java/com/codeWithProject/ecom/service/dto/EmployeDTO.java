package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeDTO {

    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateEmbauche;
    private String poste;
    private Double salaire;
    private String statut;
    private String departement;
    private Integer soldeConges;

    private Long anciennete;
    private Double salaireAnnuel;

    // Relations
    private Long serviceId;
    private String serviceCode;
    private String serviceLibelle;

    private Long managerId;
    private String managerNom;

    private Integer nombreCompetences;
    private Integer nombreFormations;
    private Integer nombreDemandesConge;

    private List<Long> competenceIds;
    private List<Long> formationIds;
    private List<Long> demandeCongeIds;
}