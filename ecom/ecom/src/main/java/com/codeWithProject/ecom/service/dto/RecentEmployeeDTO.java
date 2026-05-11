package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentEmployeeDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String poste;
    private String departement;
    private LocalDate dateEmbauche;
}