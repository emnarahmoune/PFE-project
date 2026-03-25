package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.IndicateurRH;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper pour l'entité IndicateurRH
 */
@Component
public class IndicateurRHMapper {

    public IndicateurRHDTO toDto(IndicateurRH entity) {
        if (entity == null) {
            return null;
        }

        IndicateurRHDTO dto = new IndicateurRHDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getType());
        dto.setValeur(entity.getValeur());
        dto.setDateCalcul(entity.getDateCalcul());
        dto.setPeriode(entity.getPeriode());
        dto.setAnnee(entity.getAnnee());
        dto.setMois(entity.getMois());
        dto.setTrimestre(entity.getTrimestre());
        dto.setDepartement(entity.getDepartement());
        dto.setCommentaire(entity.getCommentaire());
        dto.setTendance(entity.getTendance());
        dto.setValeurPrecedente(entity.getValeurPrecedente());
        dto.setVariationPourcentage(entity.getVariationPourcentage());

        if (entity.getSystemeBI() != null) {
            dto.setSystemeBIId(entity.getSystemeBI().getId());
            dto.setSystemeBIVersion(entity.getSystemeBI().getVersion());
        }

        // Métadonnées
        dto.setDansLaNorme(entity.isDansLaNorme());
        dto.setNiveauAlerte(entity.getNiveauAlerte());
        dto.setDescription(entity.getDescription());

        return dto;
    }

    public IndicateurRH toEntity(IndicateurRHDTO dto) {
        if (dto == null) {
            return null;
        }

        return IndicateurRH.builder()
                .id(dto.getId())
                .type(dto.getType())
                .valeur(dto.getValeur())
                .dateCalcul(dto.getDateCalcul())
                .periode(dto.getPeriode())
                .annee(dto.getAnnee())
                .mois(dto.getMois())
                .trimestre(dto.getTrimestre())
                .departement(dto.getDepartement())
                .commentaire(dto.getCommentaire())
                .tendance(dto.getTendance())
                .valeurPrecedente(dto.getValeurPrecedente())
                .build();
    }
}