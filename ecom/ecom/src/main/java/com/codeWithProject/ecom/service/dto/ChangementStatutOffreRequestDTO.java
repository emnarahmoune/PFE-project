package com.codeWithProject.ecom.service.dto;


import com.codeWithProject.ecom.entity.enums.StatutOffreRecrutement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangementStatutOffreRequestDTO {

    private StatutOffreRecrutement statut;
}
