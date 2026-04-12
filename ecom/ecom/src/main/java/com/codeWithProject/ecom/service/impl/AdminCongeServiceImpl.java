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
        List<DemandeConge> demandes = demandeRepository.findDemandesEnAttentePlusDe10Jours();
        return demandes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeAdminDTO> getAllDemandes() {
        List<DemandeConge> demandes = demandeRepository.findAll();
        return demandes.stream().map(this::convertToDTO).collect(Collectors.toList());
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
    public void validerDemande(Long demandeId, String commentaire) {
        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", demandeId));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être validées");
        }

        // Récupérer la tâche Camunda associée
        String processInstanceId = demande.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new BusinessException("Cette demande n'est pas associée à un workflow Camunda");
        }

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (tasks.isEmpty()) {
            log.warn("Aucune tâche trouvée pour l'instance {}", processInstanceId);
            // Valider directement en base sans Camunda
            demande.valider();
            demandeRepository.save(demande);

            // Créer une notification
            String message = String.format("✅ Votre demande de congé du %s au %s a été approuvée par l'Admin RH !",
                    demande.getDateDebut(), demande.getDateFin());
            notificationService.createNotification(demande.getEmploye().getId(), message, "SUCCESS", demande.getId());
            log.info("✅ Demande {} validée directement (sans Camunda)", demandeId);
            return;
        }

        // Décision RH via Camunda
        Task task = tasks.get(0);
        workflowService.processRHDecision(task.getId(), true, commentaire, null);
        log.info("✅ Demande {} validée par Admin RH", demandeId);
    }

    @Override
    @Transactional
    public void refuserDemande(Long demandeId, String motif) {
        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", demandeId));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être refusées");
        }

        // Récupérer la tâche Camunda associée
        String processInstanceId = demande.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new BusinessException("Cette demande n'est pas associée à un workflow Camunda");
        }

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (tasks.isEmpty()) {
            // Refuser directement en base sans Camunda
            demande.refuserAvecMotif(motif);
            demandeRepository.save(demande);

            // Créer une notification
            String message = String.format("❌ Votre demande de congé du %s au %s a été refusée par l'Admin RH. Motif : %s",
                    demande.getDateDebut(), demande.getDateFin(), motif);
            notificationService.createNotification(demande.getEmploye().getId(), message, "ERROR", demande.getId());
            log.info("❌ Demande {} refusée directement (sans Camunda)", demandeId);
            return;
        }

        // Décision RH via Camunda
        Task task = tasks.get(0);
        workflowService.processRHDecision(task.getId(), false, motif, null);
        log.info("❌ Demande {} refusée par Admin RH avec motif: {}", demandeId, motif);
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
        List<DemandeConge> orphanDemandes = demandeRepository.findByProcessInstanceIdIsNull();
        return orphanDemandes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Convertit une entité DemandeConge en DTO pour l'admin
     */
    private DemandeCongeAdminDTO convertToDTO(DemandeConge demande) {
        DemandeCongeAdminDTO.DemandeCongeAdminDTOBuilder builder = DemandeCongeAdminDTO.builder()
                .id(demande.getId())
                .employeNom(demande.getEmploye().getNom())
                .employePrenom(demande.getEmploye().getPrenom())
                .employeEmail(demande.getEmploye().getEmail())
                .employeId(demande.getEmploye().getId())
                .dateDebut(demande.getDateDebut())
                .dateFin(demande.getDateFin())
                .joursOuvres(demande.getJoursOuvres())
                .type(demande.getType())
                .commentaire(demande.getCommentaire())
                .statut(demande.getStatut())
                .dateDemande(demande.getDateDemande())
                .processInstanceId(demande.getProcessInstanceId())
                .currentTaskId(demande.getCurrentTaskId())  // ✅ Ajout du champ
                .motifRefus(demande.getMotifRefus());

        // Récupérer l'ID de la tâche Camunda si disponible
        if (demande.getProcessInstanceId() != null) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(demande.getProcessInstanceId())
                    .list();
            if (!tasks.isEmpty()) {
                builder.taskId(tasks.get(0).getId());
            }
        }

        return builder.build();
    }
}