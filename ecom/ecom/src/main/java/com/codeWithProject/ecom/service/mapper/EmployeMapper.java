package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Competence;
import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.service.dto.CompetenceEmployeDTO;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import com.codeWithProject.ecom.service.dto.FormationEmployeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmployeMapper {

    public EmployeDTO toDto(Employe entity) {
        if (entity == null) {
            return null;
        }

        EmployeDTO dto = new EmployeDTO();

        // ===== BASIQUE =====
        dto.setId(entity.getId());
        dto.setMatricule(entity.getMatricule());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setAdresse(entity.getAdresse());
        dto.setDateEmbauche(entity.getDateEmbauche());
        dto.setPoste(entity.getPoste());
        dto.setSalaire(entity.getSalaire());
        dto.setStatut(entity.getStatut());
        dto.setDepartement(entity.getDepartement());
        dto.setSoldeConges(entity.getSoldeConges());

        // Champs optionnels si présents dans ton DTO
        dto.setActif(entity.getActif());
        dto.setRole(entity.getRole());
        dto.setTypeEmploye(entity.getTypeEmploye());

        // ===== STATS =====
        dto.setAnciennete(entity.getAnciennete());
        dto.setSalaireAnnuel(entity.getSalaireAnnuel());

        // ===== SERVICE =====
        if (entity.getService() != null) {
            dto.setServiceId(entity.getService().getId());
            dto.setServiceCode(entity.getService().getCodeService());
            dto.setServiceLibelle(entity.getService().getLibelle());
        }

        // ===== MANAGER =====
        if (entity.getManager() != null) {
            dto.setManagerId(entity.getManager().getId());
            dto.setManagerNom(entity.getManager().getNomComplet());
        }

        // ===== FORMATIONS AVEC PROGRESSION =====
        if (entity.getEmployeFormations() != null) {
            dto.setNombreFormations(entity.getEmployeFormations().size());

            dto.setFormations(
                    entity.getEmployeFormations()
                            .stream()
                            .filter(ef -> ef.getFormation() != null)
                            .map(ef -> FormationEmployeDTO.builder()
                                    .id(ef.getFormation().getId())
                                    .titre(ef.getFormation().getTitre())
                                    .progression(ef.getProgression())
                                    .statut(ef.getStatut())
                                    .build()
                            )
                            .collect(Collectors.toList())
            );
        }

        // ===== COMPÉTENCES =====
        if (entity.getCompetences() != null) {
            dto.setNombreCompetences(entity.getCompetences().size());

            dto.setCompetences(
                    entity.getCompetences()
                            .stream()
                            .map(this::mapCompetenceToDto)
                            .collect(Collectors.toList())
            );
        }

        // ===== DEMANDES CONGÉ =====
        if (entity.getDemandesConge() != null) {
            dto.setNombreDemandesConge(entity.getDemandesConge().size());

            dto.setDemandeCongeIds(
                    entity.getDemandesConge()
                            .stream()
                            .map(DemandeConge::getId)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    public Employe toEntity(EmployeDTO dto) {
        if (dto == null) {
            return null;
        }

        return Employe.builder()
                .id(dto.getId())
                .matricule(dto.getMatricule())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .dateEmbauche(dto.getDateEmbauche())
                .poste(dto.getPoste())
                .salaire(dto.getSalaire())
                .statut(dto.getStatut() != null ? dto.getStatut() : "ACTIF")
                .departement(dto.getDepartement())
                .soldeConges(dto.getSoldeConges() != null ? dto.getSoldeConges() : 25)
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .role(dto.getRole())
                .typeEmploye(dto.getTypeEmploye())
                .build();
    }

    private CompetenceEmployeDTO mapCompetenceToDto(Competence competence) {
        CompetenceEmployeDTO dto = new CompetenceEmployeDTO();

        dto.setNom(competence.getNom());
        dto.setNiveau("INTERMEDIAIRE");

        return dto;
    }
}