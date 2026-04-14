package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.ScoreTurnover;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import org.springframework.stereotype.Component;

@Component
public class ScoreTurnoverMapper {

    public ScoreTurnoverDTO toDto(ScoreTurnover entity) {
        if (entity == null) return null;

        ScoreTurnoverDTO dto = new ScoreTurnoverDTO();
        dto.setId(entity.getId());
        dto.setScore(entity.getScore());
        dto.setNiveauRisque(entity.getNiveauRisque());
        dto.setDatePrediction(entity.getDatePrediction());
        dto.setPeriodePrediction(entity.getPeriodePrediction());
        dto.setFacteursPrincipaux(entity.getFacteursPrincipaux());
        dto.setConfianceModele(entity.getConfianceModele());
        dto.setVersionModele(entity.getVersionModele());
        dto.setActionRecommandee(entity.getActionRecommandee());

        dto.setScoreAnciennete(entity.getScoreAnciennete());
        dto.setScoreSalaire(entity.getScoreSalaire());
        dto.setScorePerformance(entity.getScorePerformance());
        dto.setScoreFormation(entity.getScoreFormation());
        dto.setScoreAbsenteisme(entity.getScoreAbsenteisme());

        // Informations employé – directement via Employe
        if (entity.getEmploye() != null) {
            dto.setEmployeId(entity.getEmploye().getId());
            dto.setEmployeMatricule(entity.getEmploye().getMatricule());
            dto.setEmployeDepartement(entity.getEmploye().getDepartement());
            dto.setEmployeNom(entity.getEmploye().getNom());
            dto.setEmployePrenom(entity.getEmploye().getPrenom());
        }

        // Informations système BI
        if (entity.getSystemeBI() != null) {
            dto.setSystemeBIId(entity.getSystemeBI().getId());
            dto.setSystemeBIVersion(entity.getSystemeBI().getVersion());
        }

        dto.setNecessiteAlerte(entity.necessiteAlerte());
        dto.setCouleurAffichage(entity.getCouleurAffichage());
        dto.setResume(entity.getResume());
        dto.setValide(entity.isValide());

        return dto;
    }

    public ScoreTurnover toEntity(ScoreTurnoverDTO dto) {
        if (dto == null) return null;

        return ScoreTurnover.builder()
                .id(dto.getId())
                .score(dto.getScore())
                .niveauRisque(dto.getNiveauRisque())
                .datePrediction(dto.getDatePrediction())
                .periodePrediction(dto.getPeriodePrediction())
                .facteursPrincipaux(dto.getFacteursPrincipaux())
                .confianceModele(dto.getConfianceModele())
                .versionModele(dto.getVersionModele())
                .actionRecommandee(dto.getActionRecommandee())
                .scoreAnciennete(dto.getScoreAnciennete())
                .scoreSalaire(dto.getScoreSalaire())
                .scorePerformance(dto.getScorePerformance())
                .scoreFormation(dto.getScoreFormation())
                .scoreAbsenteisme(dto.getScoreAbsenteisme())
                .build();
    }
}