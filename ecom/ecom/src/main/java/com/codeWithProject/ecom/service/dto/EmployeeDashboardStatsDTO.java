package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboardStatsDTO {

    private Integer congesRestants;
    private Integer formationsTerminees;
    private Double progressionFormations;
    private Integer competencesValidees;
    private Double noteMoyenne;
    private Integer certificatsObtenus;
    private Integer recommandationsIA;
}