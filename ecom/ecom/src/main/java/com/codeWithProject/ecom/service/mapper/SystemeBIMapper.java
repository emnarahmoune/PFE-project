package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.SystemeBI;
import com.codeWithProject.ecom.service.dto.SystemeBIDTO;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

/**
 * Mapper pour l'entité SystemeBI
 */
@Component
public class SystemeBIMapper {

    public SystemeBIDTO toDto(SystemeBI entity) {
        if (entity == null) {
            return null;
        }

        SystemeBIDTO dto = new SystemeBIDTO();
        dto.setId(entity.getId());
        dto.setVersion(entity.getVersion());
        dto.setOutilETL(entity.getOutilETL());
        dto.setOutilVisualisation(entity.getOutilVisualisation());
        dto.setModeleML(entity.getModeleML());
        dto.setDerniereExecution(entity.getDerniereExecution());
        dto.setStatut(entity.getStatut());

        // Statistiques
        if (entity.getIndicateurs() != null) {
            dto.setNombreIndicateurs(entity.getIndicateurs().size());
            dto.setIndicateurIds(entity.getIndicateurs().stream()
                    .map(i -> i.getId())
                    .collect(Collectors.toList()));
        }

        if (entity.getScoresTurnover() != null) {
            dto.setNombreScoresTurnover(entity.getScoresTurnover().size());
            dto.setScoreIds(entity.getScoresTurnover().stream()
                    .map(s -> s.getId())
                    .collect(Collectors.toList()));
        }

        // Métadonnées
        dto.setOperationnel(entity.isOperationnel());
        dto.setDernierResumeExecution(entity.getDerniereExecution() != null ?
                "Dernière exécution: " + entity.getDerniereExecution() : "Jamais exécuté");

        return dto;
    }

    public SystemeBI toEntity(SystemeBIDTO dto) {
        if (dto == null) {
            return null;
        }

        return SystemeBI.builder()
                .id(dto.getId())
                .version(dto.getVersion())
                .outilETL(dto.getOutilETL())
                .outilVisualisation(dto.getOutilVisualisation())
                .modeleML(dto.getModeleML())
                .derniereExecution(dto.getDerniereExecution())
                .statut(dto.getStatut() != null ? dto.getStatut() : "ACTIF")
                .build();
    }
}