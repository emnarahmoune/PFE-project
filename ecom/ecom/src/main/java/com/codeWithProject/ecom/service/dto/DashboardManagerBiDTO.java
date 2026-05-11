package com.codeWithProject.ecom.service.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardManagerBiDTO {

    private Long totalEmployes;
    private Long employesActifs;
    private Long congesEnAttente;
    private Long joursAbsence;
    private BigDecimal tauxPresence;
    private BigDecimal scoreRisqueMoyen;
    private Long employesRisqueEleve;
    private List<TopCompetenceDTO> topCompetences;

    public DashboardManagerBiDTO() {
    }

    public Long getTotalEmployes() {
        return totalEmployes;
    }

    public void setTotalEmployes(Long totalEmployes) {
        this.totalEmployes = totalEmployes;
    }

    public Long getEmployesActifs() {
        return employesActifs;
    }

    public void setEmployesActifs(Long employesActifs) {
        this.employesActifs = employesActifs;
    }

    public Long getCongesEnAttente() {
        return congesEnAttente;
    }

    public void setCongesEnAttente(Long congesEnAttente) {
        this.congesEnAttente = congesEnAttente;
    }

    public Long getJoursAbsence() {
        return joursAbsence;
    }

    public void setJoursAbsence(Long joursAbsence) {
        this.joursAbsence = joursAbsence;
    }

    public BigDecimal getTauxPresence() {
        return tauxPresence;
    }

    public void setTauxPresence(BigDecimal tauxPresence) {
        this.tauxPresence = tauxPresence;
    }

    public BigDecimal getScoreRisqueMoyen() {
        return scoreRisqueMoyen;
    }

    public void setScoreRisqueMoyen(BigDecimal scoreRisqueMoyen) {
        this.scoreRisqueMoyen = scoreRisqueMoyen;
    }

    public Long getEmployesRisqueEleve() {
        return employesRisqueEleve;
    }

    public void setEmployesRisqueEleve(Long employesRisqueEleve) {
        this.employesRisqueEleve = employesRisqueEleve;
    }

    public List<TopCompetenceDTO> getTopCompetences() {
        return topCompetences;
    }

    public void setTopCompetences(List<TopCompetenceDTO> topCompetences) {
        this.topCompetences = topCompetences;
    }
}