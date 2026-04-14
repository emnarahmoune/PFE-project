package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.AdminCongeService;
import com.codeWithProject.ecom.service.NotificationService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCongeServiceImpl implements AdminCongeService {

    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;
    private final TaskService taskService;
    private final WorkflowService workflowService;

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeAdminDTO> getDemandesEnAttentePlusDe10Jours() {
        log.info("Récupération des demandes avec >10 jours en attente");
        List<DemandeConge> demandes = demandeRepository.findDemandesEnAttentePlusDe10Jours();
        log.info("Nombre de demandes trouvées en base : {}", demandes.size());
        return demandes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeAdminDTO> getAllDemandes() {
        return demandeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeCongeAdminDTO getDemandeById(Long id) {
        DemandeConge demande = demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", id));
        return convertToDTO(demande);
    }

    @Override
    @Transactional
    public void validerDemande(Long demandeId, String commentaire, String adminEmail) {
        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", demandeId));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être validées");
        }

        String processInstanceId = demande.getProcessInstanceId();
        if (processInstanceId == null) {
            // Pas de workflow, validation directe
            log.warn("Demande {} sans processInstanceId, validation directe", demandeId);
            demande.valider();
            demandeRepository.save(demande);
            notificationService.createNotification(demande.getEmploye().getId(),
                    "✅ Votre demande de congé a été approuvée par l'Admin RH", "SUCCESS", demandeId);
            return;
        }

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        if (tasks.isEmpty()) {
            log.warn("Aucune tâche trouvée pour l'instance {}, validation directe", processInstanceId);
            demande.valider();
            demandeRepository.save(demande);
            notificationService.createNotification(demande.getEmploye().getId(),
                    "✅ Votre demande de congé a été approuvée par l'Admin RH", "SUCCESS", demandeId);
            return;
        }

        Task task = tasks.get(0);
        // Passer l'email de l'admin pour la vérification dans le workflow
        workflowService.processRHDecision(task.getId(), true, commentaire, adminEmail);
        log.info("✅ Demande {} validée via Camunda par {}", demandeId, adminEmail);
    }

    @Override
    @Transactional
    public void refuserDemande(Long demandeId, String motif, String adminEmail) {
        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", demandeId));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être refusées");
        }

        String processInstanceId = demande.getProcessInstanceId();
        if (processInstanceId == null) {
            log.warn("Demande {} sans processInstanceId, refus direct", demandeId);
            demande.refuserAvecMotif(motif);
            demandeRepository.save(demande);
            notificationService.createNotification(demande.getEmploye().getId(),
                    "❌ Votre demande de congé a été refusée. Motif : " + motif, "ERROR", demandeId);
            return;
        }

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        if (tasks.isEmpty()) {
            log.warn("Aucune tâche trouvée pour l'instance {}, refus direct", processInstanceId);
            demande.refuserAvecMotif(motif);
            demandeRepository.save(demande);
            notificationService.createNotification(demande.getEmploye().getId(),
                    "❌ Votre demande de congé a été refusée. Motif : " + motif, "ERROR", demandeId);
            return;
        }

        Task task = tasks.get(0);
        workflowService.processRHDecision(task.getId(), false, motif, adminEmail);
        log.info("❌ Demande {} refusée via Camunda par {}", demandeId, adminEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatsByStatut() {
        List<Object[]> results = demandeRepository.countByStatut();
        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            stats.put((String) result[0], (Long) result[1]);
        }
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeAdminDTO> getOrphanRequests() {
        return demandeRepository.findByProcessInstanceIdIsNull().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private DemandeCongeAdminDTO convertToDTO(DemandeConge demande) {
        try {
            DemandeCongeAdminDTO.DemandeCongeAdminDTOBuilder builder = DemandeCongeAdminDTO.builder()
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
                    .processInstanceId(demande.getProcessInstanceId())
                    .currentTaskId(demande.getCurrentTaskId());

            if (demande.getEmploye() != null) {
                builder.employeId(demande.getEmploye().getId())
                        .employeNom(demande.getEmploye().getNom())
                        .employePrenom(demande.getEmploye().getPrenom())
                        .employeEmail(demande.getEmploye().getEmail());
            }

            if (demande.getManager() != null) {
                builder.managerId(demande.getManager().getId())
                        .managerNom(demande.getManager().getNom());
            }

            // Récupération de la tâche Camunda active (optionnel)
            String taskId = null;
            if (demande.getProcessInstanceId() != null) {
                try {
                    List<Task> tasks = taskService.createTaskQuery()
                            .processInstanceId(demande.getProcessInstanceId())
                            .list();
                    if (!tasks.isEmpty()) {
                        taskId = tasks.get(0).getId();
                    }
                } catch (Exception e) {
                    log.warn("Impossible de récupérer la tâche pour l'instance {} : {}",
                            demande.getProcessInstanceId(), e.getMessage());
                }
            }
            builder.taskId(taskId != null ? taskId : demande.getCurrentTaskId());

            return builder.build();
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de la demande {} : {}", demande.getId(), e.getMessage(), e);
            return DemandeCongeAdminDTO.builder()
                    .id(demande.getId())
                    .statut(demande.getStatut())
                    .joursOuvres(demande.getJoursOuvres())
                    .build();
        }
    }
}