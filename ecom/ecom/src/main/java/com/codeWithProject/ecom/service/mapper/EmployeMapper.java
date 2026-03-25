package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmployeMapper {

    public EmployeDTO toDto(Employe entity) {
        if (entity == null) return null;

        EmployeDTO dto = new EmployeDTO();
        dto.setId(entity.getId());
        dto.setMatricule(entity.getMatricule());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setDateEmbauche(entity.getDateEmbauche());
        dto.setPoste(entity.getPoste());
        dto.setSalaire(entity.getSalaire());
        dto.setStatut(entity.getStatut());
        dto.setDepartement(entity.getDepartement());
        dto.setSoldeConges(entity.getSoldeConges());

        // Ancienneté et salaire annuel
        dto.setAnciennete(entity.getAnciennete());
        dto.setSalaireAnnuel(entity.getSalaireAnnuel());

        // Service
        if (entity.getService() != null) {
            dto.setServiceId(entity.getService().getId());
            dto.setServiceCode(entity.getService().getCodeService());
            dto.setServiceLibelle(entity.getService().getLibelle());
        }

        // Manager
        if (entity.getManager() != null) {
            dto.setManagerId(entity.getManager().getId());
            dto.setManagerNom(entity.getManager().getNomComplet());
        }

        // Compétences
        if (entity.getCompetences() != null) {
            dto.setNombreCompetences(entity.getCompetences().size());
            dto.setCompetenceIds(entity.getCompetences().stream()
                    .map(ec -> ec.getCompetence().getId())
                    .collect(Collectors.toList()));
        }

        // Formations
        if (entity.getFormations() != null) {
            dto.setNombreFormations(entity.getFormations().size());
            dto.setFormationIds(entity.getFormations().stream()
                    .map(Formation::getId)
                    .collect(Collectors.toList()));
        }

        // Demandes de congé
        if (entity.getDemandesConge() != null) {
            dto.setNombreDemandesConge(entity.getDemandesConge().size());
            dto.setDemandeCongeIds(entity.getDemandesConge().stream()
                    .map(DemandeConge::getId)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Employe toEntity(EmployeDTO dto) {
        if (dto == null) return null;

        return Employe.builder()
                .id(dto.getId())
                .matricule(dto.getMatricule())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .dateEmbauche(dto.getDateEmbauche())
                .poste(dto.getPoste())
                .salaire(dto.getSalaire())
                .statut(dto.getStatut() != null ? dto.getStatut() : "ACTIF")
                .departement(dto.getDepartement())
                .soldeConges(dto.getSoldeConges() != null ? dto.getSoldeConges() : 25)
                .build();
    }
}