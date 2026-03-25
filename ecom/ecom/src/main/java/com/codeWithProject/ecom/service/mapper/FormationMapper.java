package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

/**
 * Mapper pour l'entité Formation
 */
@Component
public class FormationMapper {

    public FormationDTO toDto(Formation entity) {
        if (entity == null) {
            return null;
        }

        FormationDTO dto = new FormationDTO();
        dto.setId(entity.getId());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        dto.setDomaine(entity.getDomaine());
        dto.setDureeHeures(entity.getDureeHeures());
        dto.setActif(entity.getActif());
        dto.setDateCreation(entity.getDateCreation());

        // Participants
        if (entity.getParticipants() != null) {
            dto.setNombreParticipants(entity.getParticipants().size());
            dto.setParticipantIds(entity.getParticipants().stream()
                    .map(Employe::getId)
                    .collect(Collectors.toList()));
        }

        // Résumé pour affichage
        dto.setResume(String.format("%s (%dh) - %d participants",
                entity.getTitre(),
                entity.getDureeHeures() != null ? entity.getDureeHeures() : 0,
                dto.getNombreParticipants() != null ? dto.getNombreParticipants() : 0));

        return dto;
    }

    public Formation toEntity(FormationDTO dto) {
        if (dto == null) {
            return null;
        }

        return Formation.builder()
                .id(dto.getId())
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .domaine(dto.getDomaine())
                .dureeHeures(dto.getDureeHeures())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
    }
}