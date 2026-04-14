package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
import org.springframework.stereotype.Component;

@Component
public class DemandeCongeMapper {

    public DemandeCongeDTO toDto(DemandeConge entity) {
        if (entity == null) return null;

        DemandeCongeDTO dto = new DemandeCongeDTO();
        dto.setId(entity.getId());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setType(entity.getType());
        dto.setStatut(entity.getStatut());
        dto.setDateDemande(entity.getDateDemande());
        dto.setDateDecision(entity.getDateDecision());
        dto.setCommentaire(entity.getCommentaire());
        dto.setMotifRefus(entity.getMotifRefus());
        dto.setJoursOuvres(entity.getJoursOuvres());
        dto.setUrgente(entity.getUrgente());
        dto.setNombreJours(entity.getNombreJoursCalendaires());
        dto.setResume(entity.getResume());
        dto.setProcessInstanceId(entity.getProcessInstanceId());

        // Employé (directement via Employe, plus d'Utilisateur intermédiaire)
        if (entity.getEmploye() != null) {
            dto.setEmployeId(entity.getEmploye().getId());
            dto.setEmployeMatricule(entity.getEmploye().getMatricule());
            dto.setEmployeNom(entity.getEmploye().getNom());
            dto.setEmployePrenom(entity.getEmploye().getPrenom());
        }

        // Manager
        if (entity.getManager() != null) {
            dto.setManagerId(entity.getManager().getId());
            dto.setManagerNom(entity.getManager().getNomComplet());
            dto.setManagerEmail(entity.getManager().getEmail());
        }

        // Champs workflow (à remplir par le service si besoin)
        // dto.setAdminEmail(...); dto.setManagerApprouve(...); etc.

        return dto;
    }

    public DemandeConge toEntity(DemandeCongeDTO dto) {
        if (dto == null) return null;

        return DemandeConge.builder()
                .id(dto.getId())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .type(dto.getType())
                .statut(dto.getStatut())
                .dateDemande(dto.getDateDemande())
                .dateDecision(dto.getDateDecision())
                .commentaire(dto.getCommentaire())
                .motifRefus(dto.getMotifRefus())
                .joursOuvres(dto.getJoursOuvres())
                .urgente(dto.getUrgente() != null ? dto.getUrgente() : false)
                .processInstanceId(dto.getProcessInstanceId())
                .build();
    }
}