package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.AdminCongeService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusDetailsDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusManagerDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCongeServiceImpl implements AdminCongeService {

    private final DemandeCongeRepository demandeRepository;
    private final WorkflowService workflowService;
    private final TaskService taskService;

    @Override
    public List<DemandeCongeAdminDTO> getDemandesEnAttentePlusDe10Jours() {
        List<DemandeConge> demandes = demandeRepository.findByStatutAndJoursOuvresGreaterThan("EN_ATTENTE_RH", 10);
        return demandes.stream().map(DemandeCongeAdminDTO::fromEntity).collect(Collectors.toList());
    }

@Override
@Transactional(readOnly = true)
public List<DemandeCongeAdminDTO> getAllDemandes() {
    log.info("Récupération de toutes les demandes de congé");

    return demandeRepository.findAll()
            .stream()
            .map(demande -> {
                try {
                    return DemandeCongeAdminDTO.fromEntity(demande);
                } catch (Exception e) {
                    log.error(
                            "ERREUR MAPPING /all - demandeId={}, statut={}, employeId={}, managerId={}, adminRhId={}",
                            demande.getId(),
                            demande.getStatut(),
                            demande.getEmploye() != null ? demande.getEmploye().getId() : null,
                            demande.getManager() != null ? demande.getManager().getId() : null,
                            demande.getAdminRh() != null ? demande.getAdminRh().getId() : null,
                            e
                    );

                    throw e;
                }
            })
            .collect(Collectors.toList());
}


private DemandeCongeAdminDTO toSafeAdminDto(DemandeConge d) {
    DemandeCongeAdminDTO dto = new DemandeCongeAdminDTO();

    dto.setId(d.getId());

    dto.setDateDebut(d.getDateDebut());
    dto.setDateFin(d.getDateFin());
    dto.setDateDemande(d.getDateDemande());
    dto.setDateDecision(d.getDateDecision());

    dto.setStatut(d.getStatut());
    dto.setType(d.getType());
    dto.setCommentaire(d.getCommentaire());
    dto.setMotifRefus(d.getMotifRefus());


    dto.setProcessInstanceId(d.getProcessInstanceId());
    dto.setCurrentTaskId(d.getCurrentTaskId());
    dto.setTaskId(d.getCurrentTaskId());

    dto.setUrgente(Boolean.TRUE.equals(d.getUrgente()));

    if (d.getEmploye() != null) {
        dto.setEmployeId(d.getEmploye().getId());
        dto.setEmployeNom(d.getEmploye().getNom());
        dto.setEmployePrenom(d.getEmploye().getPrenom());
        dto.setEmployeEmail(d.getEmploye().getEmail());
    }

    if (d.getManager() != null) {
        dto.setManagerId(d.getManager().getId());
        dto.setManagerNom(d.getManager().getNom());
    }

    if (d.getAdminRh() != null) {
        dto.setAdminRhId(d.getAdminRh().getId());
        dto.setAdminRhNom(d.getAdminRh().getNom());
    }

    return dto;
}


private void verifierPasAutoTraitement(DemandeConge demande, String userEmail) {
    if (
            demande != null &&
            demande.getEmploye() != null &&
            demande.getEmploye().getEmail() != null &&
            userEmail != null &&
            demande.getEmploye().getEmail().equalsIgnoreCase(userEmail)
    ) {
        throw new BusinessException("Vous ne pouvez pas traiter votre propre demande de congé.");
    }
}
    @Override
    public DemandeCongeAdminDTO getDemandeById(Long id) {
        DemandeConge demande = demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));
        return DemandeCongeAdminDTO.fromEntity(demande);
    }

    @Override
    @Transactional
    public void validerDemande(Long demandeId, String commentaire, String adminEmail) {
        log.info("=== VALIDATION DEMANDE ID {} par admin {} ===", demandeId, adminEmail);

        DemandeConge demande = demandeRepository.findById(demandeId)
        .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

verifierPasAutoTraitement(demande, adminEmail);

log.debug("Statut de la demande avant traitement : {}", demande.getStatut());

        if (!"EN_ATTENTE_RH".equals(demande.getStatut()) && !"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente (EN_ATTENTE ou EN_ATTENTE_RH) peuvent être approuvées. Statut actuel : " + demande.getStatut());
        }

        if ("EN_ATTENTE".equals(demande.getStatut())) {
            log.warn("Demande {} encore en EN_ATTENTE, passage forcé à EN_ATTENTE_RH", demandeId);
            demande.setStatut("EN_ATTENTE_RH");
            demandeRepository.save(demande);
        }

        Task task = findActiveRhTask(demande, adminEmail);


        workflowService.processRHDecision(task.getId(), true, commentaire, adminEmail);
        log.info("Décision RH d'approbation envoyée pour la tâche {}", task.getId());
    }

    @Override
    @Transactional
    public void refuserDemande(Long demandeId, String motif, String adminEmail) {
        log.info("=== REFUS DEMANDE ID {} par admin {} ===", demandeId, adminEmail);

        DemandeConge demande = demandeRepository.findById(demandeId)
        .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

verifierPasAutoTraitement(demande, adminEmail);

if (!"EN_ATTENTE_RH".equals(demande.getStatut()) && !"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente (EN_ATTENTE ou EN_ATTENTE_RH) peuvent être refusées");
        }

        if ("EN_ATTENTE".equals(demande.getStatut())) {
            demande.setStatut("EN_ATTENTE_RH");
            demandeRepository.save(demande);
        }

        Task task = findActiveRhTask(demande, adminEmail);

        workflowService.processRHDecision(task.getId(), false, motif, adminEmail);
        log.info("Décision RH de refus envoyée pour la tâche {}", task.getId());
    }

    @Override
    public Map<String, Long> getStatsByStatut() {
        return demandeRepository.findAll().stream()
                .collect(Collectors.groupingBy(DemandeConge::getStatut, Collectors.counting()));
    }

    @Override
    public List<DemandeCongeAdminDTO> getOrphanRequests() {
        return demandeRepository.findByProcessInstanceIdIsNull().stream()
                .map(DemandeCongeAdminDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalendarEventDTO> getAllCalendarEvents() {
        return demandeRepository.findAllForCalendar();
    }
    @Override
    public List<DemandeRefusManagerDTO> getDemandesRefuseesParManager() {
        return demandeRepository.findDemandesRefuseesParManager();
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeRefusDetailsDTO getRefusDetails(Long demandeId) {
        log.info("Récupération des détails de refus pour la demande ID: {}", demandeId);

        DemandeRefusDetailsDTO details = demandeRepository.findRefusDetailsById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande refusée non trouvée avec l'ID: " + demandeId));

        log.debug("Détails trouvés: employé={} {}, type={}", details.getEmployePrenom(), details.getEmployeNom(), details.getType());
        return details;
    }

private Task findActiveRhTask(DemandeConge demande, String adminEmail) {
    String taskId = demande.getCurrentTaskId();

    if (taskId != null && !taskId.isBlank()) {
        Task taskById = taskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();

        if (taskById != null) {
    log.info("Tâche RH trouvée par currentTaskId {} : assignee={}", taskId, taskById.getAssignee());

    String assignee = taskById.getAssignee();

    if (assignee == null || assignee.isBlank()) {
        taskService.claim(taskById.getId(), adminEmail);
        log.info("Tâche RH {} claimée par {}", taskById.getId(), adminEmail);
    } else if (!adminEmail.equalsIgnoreCase(assignee)) {
        log.warn(
                "Tâche RH {} assignée à {}, reprise par admin RH {}",
                taskById.getId(),
                assignee,
                adminEmail
        );

        taskService.setAssignee(taskById.getId(), adminEmail);
    }

    return taskById;
}

        log.warn("Aucune tâche active trouvée avec taskId={}, fallback processInstanceId", taskId);
    }

    String processInstanceId = demande.getProcessInstanceId();

    if (processInstanceId == null || processInstanceId.isBlank()) {
        throw new BusinessException("Demande sans instance Camunda, impossible de traiter via workflow");
    }

    Task task = taskService.createTaskQuery()
            .processInstanceId(processInstanceId)
            .taskAssignee(adminEmail)
            .active()
            .singleResult();

    if (task != null) {
        return task;
    }

    log.warn(
            "Aucune tâche assignée à {} pour l'instance {}, recherche sans assignee",
            adminEmail,
            processInstanceId
    );

    task = taskService.createTaskQuery()
            .processInstanceId(processInstanceId)
            .active()
            .singleResult();

    if (task == null) {
        throw new BusinessException("Aucune tâche active trouvée pour cette demande");
    }

    if (task.getAssignee() == null || task.getAssignee().isBlank()) {
        taskService.claim(task.getId(), adminEmail);
        log.info("Tâche RH {} claimée par {}", task.getId(), adminEmail);
    }

    return task;
}

}