package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Competence;
import com.codeWithProject.ecom.service.dto.CompetenceDTO;
import org.springframework.stereotype.Component;

@Component
public class CompetenceMapper {

    public CompetenceDTO toDto(Competence entity) {
        if (entity == null) {
            return null;
        }

        CompetenceDTO dto = new CompetenceDTO();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setDescription(entity.getDescription());
        dto.setCategorie(entity.getCategorie());
        dto.setNombreEmployes(entity.getEmployeCompetences() != null ?
                (long) entity.getEmployeCompetences().size() : 0L);

        return dto;
    }

    public Competence toEntity(CompetenceDTO dto) {
        if (dto == null) {
            return null;
        }

        return Competence.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .description(dto.getDescription())
                .categorie(dto.getCategorie())
                .build();
    }
}