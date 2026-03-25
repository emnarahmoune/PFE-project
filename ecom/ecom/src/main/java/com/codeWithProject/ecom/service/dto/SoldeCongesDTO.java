package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldeCongesDTO {
    private Integer total;
    private Integer pris;
    private Integer restant;
    private Integer enAttente;
}