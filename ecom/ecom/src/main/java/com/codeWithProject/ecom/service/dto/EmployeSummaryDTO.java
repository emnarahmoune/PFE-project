package com.codeWithProject.ecom.service.dto;

import com.codeWithProject.ecom.entity.Employe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeSummaryDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String departement;
    private String poste;
private String photoUrl;
private String employePhotoProfil;
    public static EmployeSummaryDTO fromEntity(Employe employe) {
        if (employe == null) {
            return null;
        }

        return EmployeSummaryDTO.builder()
                .id(employe.getId())
                .nom(employe.getNom())
                .prenom(employe.getPrenom())
                .email(employe.getEmail())
                .departement(employe.getDepartement())
                .poste(employe.getPoste())
                .photoUrl(employe.getPhotoUrl())
                .employePhotoProfil(employe.getPhotoUrl())
                .build();
    }
}