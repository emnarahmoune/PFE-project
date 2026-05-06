package com.codeWithProject.ecom.service.dto;

import com.codeWithProject.ecom.entity.DemandeConge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCongeAdminDTO {

    private Long id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String type;
    private String statut;
    private LocalDate dateDemande;
    private String commentaire;

    private Long adminRhId;
    private String adminRhNom;

    private LocalDate dateDecision;
    private String motifRefus;
    private Integer joursOuvres;
    private Boolean urgente;

    private Long employeId;
    private String employeNom;
    private String employePrenom;
    private String employeEmail;
    private String employePhotoUrl;
    private String employePhotoProfil;

    private Long managerId;
    private String managerNom;

    private String processInstanceId;
    private String currentTaskId;
    private String taskId;

    private Integer joursUrgenceNonCouverts;

    /**
     * Fabrique un DTO à partir de l'entité, sans appel externe.
     * Préférer l'utilisation dans le service pour gérer les tâches Camunda.
     */
    public static DemandeCongeAdminDTO fromEntity(DemandeConge demande) {
        if (demande == null) {
            return null;
        }

        return DemandeCongeAdminDTO.builder()
                .id(demande.getId())
                .dateDebut(demande.getDateDebut())
                .dateFin(demande.getDateFin())
                .type(demande.getType())
                .statut(demande.getStatut())
                .dateDemande(demande.getDateDemande())
                .commentaire(demande.getCommentaire())
                .dateDecision(demande.getDateDecision())
                .motifRefus(demande.getMotifRefus())
                .joursOuvres(demande.getJoursOuvres())
                .urgente(demande.getUrgente())

                .employeId(demande.getEmploye() != null ? demande.getEmploye().getId() : null)
                .employeNom(demande.getEmploye() != null ? demande.getEmploye().getNom() : null)
                .employePrenom(demande.getEmploye() != null ? demande.getEmploye().getPrenom() : null)
                .employeEmail(demande.getEmploye() != null ? demande.getEmploye().getEmail() : null)
                .employePhotoProfil(demande.getEmploye() != null ? demande.getEmploye().getPhotoUrl() : null)
                .employePhotoUrl(demande.getEmploye() != null ? demande.getEmploye().getPhotoUrl() : null)

                .managerId(demande.getManager() != null ? demande.getManager().getId() : null)
                .managerNom(demande.getManager() != null ? demande.getManager().getNom() : null)

                .adminRhId(demande.getAdminRh() != null ? demande.getAdminRh().getId() : null)
                .adminRhNom(demande.getAdminRh() != null ? demande.getAdminRh().getNom() : null)

                .processInstanceId(demande.getProcessInstanceId())
                .currentTaskId(demande.getCurrentTaskId())
                .taskId(demande.getCurrentTaskId())

                .joursUrgenceNonCouverts(demande.getJoursUrgenceNonCouverts())

                .build();
    }
}