package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetenceEmployeDTO {
    private Long id;
    private String nom;
    private String categorie;
    private String niveau;
    private Boolean certifie;
    private LocalDate dateAcquisition;
    private LocalDate dateExpiration;
    private Long competenceId;
    private String description;
    private String photoUrl;
private String employePhotoProfil;

}