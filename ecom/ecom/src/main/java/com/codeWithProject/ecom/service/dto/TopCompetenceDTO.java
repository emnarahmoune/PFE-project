package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCompetenceDTO {

    private String nom;
    private Long count;
    private Double pourcentage;
}