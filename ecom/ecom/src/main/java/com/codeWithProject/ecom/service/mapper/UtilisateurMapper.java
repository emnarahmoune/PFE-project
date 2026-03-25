package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.service.dto.UtilisateurDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper pour l'entité Utilisateur
 */
@Component
public class UtilisateurMapper {

    public UtilisateurDTO toDto(Utilisateur entity) {
        if (entity == null) {
            return null;
        }

        return UtilisateurDTO.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .email(entity.getEmail())
                .telephone(entity.getTelephone())
                .actif(entity.getActif())
                .dateCreation(entity.getDateCreation())
                .typeUtilisateur(entity.getTypeUtilisateur())
                .build();
    }

    public Utilisateur toEntity(UtilisateurDTO dto) {
        if (dto == null) {
            return null;
        }

        // Note: Utilisateur est abstrait, donc cette méthode sera principalement utilisée
        // par les mappers des classes filles (EmployeMapper, ManagerMapper, etc.)
        return null;
    }
}