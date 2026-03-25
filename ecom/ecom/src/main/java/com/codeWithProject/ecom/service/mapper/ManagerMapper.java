package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.service.dto.ManagerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

/**
 * Mapper pour l'entité Manager
 */
@Component
@RequiredArgsConstructor
public class ManagerMapper {

    private final UtilisateurMapper utilisateurMapper;

    public ManagerDTO toDto(Manager entity) {
        if (entity == null) {
            return null;
        }

        ManagerDTO dto = new ManagerDTO();
        dto.setId(entity.getId());
        dto.setDepartement(entity.getDepartement());
        dto.setDateNomination(entity.getDateNomination());
        dto.setActif(entity.getActif());

        // Informations utilisateur
        dto.setUtilisateurId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());

        // Informations employé associé
        if (entity.getEmploye() != null) {
            dto.setEmployeId(entity.getEmploye().getId());
            dto.setEmployeMatricule(entity.getEmploye().getMatricule());
            if (entity.getEmploye().getUtilisateur() != null) {
                dto.setEmployeNom(entity.getEmploye().getUtilisateur().getNom());
                dto.setEmployePrenom(entity.getEmploye().getUtilisateur().getPrenom());
            }
        }

        // Statistiques
        if (entity.getEmployesGeres() != null) {
            dto.setNombreEmployesGeres(entity.getNombreEmployesGeres());
            dto.setNombreEmployesTotal(entity.getNombreEmployesTotal());
            dto.setEmployesGeresIds(entity.getEmployesGeres().stream()
                    .map(employe -> employe.getId())
                    .collect(Collectors.toList()));
        }

        if (entity.getDemandesCongeAValider() != null) {
            dto.setNombreDemandesEnAttente(entity.getNombreDemandesEnAttente());
            dto.setDemandesEnAttenteIds(entity.getDemandesCongeAValider().stream()
                    .filter(d -> "EN_ATTENTE".equals(d.getStatut()))
                    .map(d -> d.getId())
                    .collect(Collectors.toList()));

            // Demandes urgentes
            var urgentes = entity.getDemandesUrgentes();
            dto.setNombreDemandesUrgentes(urgentes.size());
            dto.setADesDemandesUrgentes(!urgentes.isEmpty());
        }

        // Métadonnées
        dto.setNomComplet(entity.getNomComplet());
        dto.setMatricule(entity.getMatricule());
        dto.setAncienneteManager(entity.getAncienneteManager());

        return dto;
    }

    public Manager toEntity(ManagerDTO dto) {
        if (dto == null) {
            return null;
        }

        return Manager.builder()
                .id(dto.getId())
                .departement(dto.getDepartement())
                .dateNomination(dto.getDateNomination())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
        // Les relations seront gérées par le service
    }
}