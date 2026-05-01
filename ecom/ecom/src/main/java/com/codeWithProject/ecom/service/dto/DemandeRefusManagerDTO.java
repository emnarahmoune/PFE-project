package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DemandeRefusManagerDTO {
    private Long id;
    private String employePrenom;
    private String employeNom;
    private String employeDepartement;
    private String managerNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motifRefus;
}