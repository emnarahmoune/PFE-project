package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAdminBiDTO {

    private Long effectifTotal;
    private Long employesActifs;
    private Long employesInactifs;
    private Long totalManagers;
    private Long totalDepartements;

    private BigDecimal masseSalariale;
    private BigDecimal salaireMoyen;

    private Long totalDemandesConge;
    private Long congesEnAttente;
    private Long congesApprouves;
    private Long joursAbsence;

    private Long totalEvaluations;
    private BigDecimal noteMoyenneGlobale;

    private Long totalFormations;
    private Long totalCompetences;

    private BigDecimal scoreRisqueMoyen;
    private Long employesRisqueEleve;

    private Map<String, Long> parDepartement;
    private Map<String, Long> repartitionStatut;
    private List<TopCompetenceDTO> topCompetences;
    private List<RecentEmployeeDTO> recentEmployees;
}