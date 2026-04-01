package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableauBordEmployeDTO {
    private Long total;
    private Long actifs;
    private Long inactifs;
    private Long enConge;
    private Double salaireMoyen;
    private Double masseSalariale;
    private Double soldeCongesMoyen;
}