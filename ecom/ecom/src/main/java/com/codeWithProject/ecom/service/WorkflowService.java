package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final DemandeCongeRepository demandeRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final AdministrateurRHRepository administrateurRHRepository;

    // ==================== MANAGER ====================

  @Transactional(readOnly = true)
public List<Map<String, Object>> getManagerTasks(String managerEmail) {
    Employe employe = employeRepository.findByEmail(managerEmail)
            .orElseThrow(() -> new BusinessException("Employé non trouvé: " + managerEmail));

    if (!"MANAGER".equalsIgnoreCase(employe.getRole())) {
        throw new BusinessException("Accès non autorisé: vous n'avez pas le rôle manager");
    }

    List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee(managerEmail)
            .active()
            .orderByTaskCreateTime()
            .desc()
            .list()
            .stream()
            /*
             * Le manager ne doit pas gérer sa propre demande.
             * Sa demande doit aller côté RH/admin.
             */
            .filter(task -> !isOwnRequest(task, managerEmail))
            .collect(Collectors.toList());

    log.info(
            "Manager {} a {} tâche(s) en attente après exclusion de ses propres demandes",
            managerEmail,
            tasks.size()
    );

    return tasks.stream()
            .map(this::mapTaskToMap)
            .collect(Collectors.toList());
}

    // ==================== ADMIN RH ====================

@Transactional(readOnly = true)
public List<Map<String, Object>> getRHTasks(String adminEmail) {
    List<Task> tasks = taskService.createTaskQuery()
            .active()
            .orderByTaskCreateTime()
            .desc()
            .list()
            .stream()
            .filter(task -> {
                String name = task.getName();
                return name != null && name.toLowerCase().contains("rh");
            })
            /*
             * L'admin connecté ne voit pas sa propre demande.
             * Mais les autres admins RH la verront.
             */
            .filter(task -> !isOwnRequest(task, adminEmail))
            .collect(Collectors.toList());

    log.info(
            "Admin RH {} voit {} tâche(s) RH après exclusion de ses propres demandes",
            adminEmail,
            tasks.size()
    );

    return tasks.stream()
            .map(this::mapTaskToMap)
            .collect(Collectors.toList());
}

    // ==================== MAPPING ====================

    private Map<String, Object> mapTaskToMap(Task task) {
        Map<String, Object> taskInfo = new HashMap<>();

        taskInfo.put("taskId", task.getId());
        taskInfo.put("taskName", task.getName());
        taskInfo.put("createTime", task.getCreateTime());
        taskInfo.put("processInstanceId", task.getProcessInstanceId());
        taskInfo.put("assignee", task.getAssignee());

        Map<String, Object> vars = new HashMap<>();

        try {
            vars = runtimeService.getVariables(task.getProcessInstanceId());
        } catch (Exception e) {
            log.warn("Impossible de récupérer les variables Camunda: {}", e.getMessage());
        }

        taskInfo.put("employeId", vars.get("employeId"));
        taskInfo.put("nbJours", vars.get("nbJours"));
        taskInfo.put("demandeId", vars.get("demandeId"));
        taskInfo.put("montantConge", vars.get("montantConge"));

        Long demandeId = vars.get("demandeId") != null
                ? Long.valueOf(vars.get("demandeId").toString())
                : null;

        if (demandeId != null) {
            demandeRepository.findById(demandeId).ifPresent(demande -> {
                taskInfo.put("dateDebut", demande.getDateDebut() != null ? demande.getDateDebut().toString() : null);
                taskInfo.put("dateFin", demande.getDateFin() != null ? demande.getDateFin().toString() : null);
                taskInfo.put("type", demande.getType());
                taskInfo.put("commentaire", demande.getCommentaire());
                taskInfo.put("statut", demande.getStatut());
                taskInfo.put("urgente", Boolean.TRUE.equals(demande.getUrgente()));
                taskInfo.put("dateDemande", demande.getDateDemande() != null ? demande.getDateDemande().toString() : null);
                taskInfo.put("dateSoumission", demande.getDateSoumission() != null ? demande.getDateSoumission().toString() : null);

                if (demande.getEmploye() != null) {
                    taskInfo.put("employeNom", demande.getEmploye().getNom());
                    taskInfo.put("employePrenom", demande.getEmploye().getPrenom());
                    taskInfo.put("employeEmail", demande.getEmploye().getEmail());
                    taskInfo.put("employeDepartement", demande.getEmploye().getDepartement());
                }
            });
        }

        return taskInfo;
    }

    private Map<String, Object> mapDemandeToMap(DemandeConge d) {
        Map<String, Object> map = new HashMap<>();

        map.put("taskId", d.getCurrentTaskId());
        map.put("taskName", "Validation RH");
        map.put("createTime", d.getDateSoumission());
        map.put("processInstanceId", d.getProcessInstanceId());
        map.put("employeId", d.getEmploye() != null ? d.getEmploye().getId() : null);
        map.put("nbJours", d.getJoursOuvres());
        map.put("demandeId", d.getId());
        map.put("dateDebut", d.getDateDebut() != null ? d.getDateDebut().toString() : null);
        map.put("dateFin", d.getDateFin() != null ? d.getDateFin().toString() : null);
        map.put("type", d.getType());
        map.put("commentaire", d.getCommentaire());
        map.put("statut", d.getStatut());
        map.put("urgente", Boolean.TRUE.equals(d.getUrgente()));
        map.put("dateDemande", d.getDateDemande() != null ? d.getDateDemande().toString() : null);
        map.put("dateSoumission", d.getDateSoumission() != null ? d.getDateSoumission().toString() : null);

        if (d.getEmploye() != null) {
            map.put("employeNom", d.getEmploye().getNom());
            map.put("employePrenom", d.getEmploye().getPrenom());
            map.put("employeEmail", d.getEmploye().getEmail());
            map.put("employeDepartement", d.getEmploye().getDepartement());
        }

        return map;
    }




    private boolean isRoleManagerOuAdmin(Employe employe) {
    if (employe == null || employe.getRole() == null) {
        return false;
    }

    String role = employe.getRole().trim().toUpperCase();

    return role.equals("MANAGER")
            || role.equals("ADMIN")
            || role.equals("ADMIN_RH")
            || role.equals("RH");
}

private boolean isOwnRequest(Task task, String userEmail) {
    if (task == null || userEmail == null || userEmail.isBlank()) {
        return false;
    }

    try {
        Map<String, Object> vars = runtimeService.getVariables(task.getProcessInstanceId());

        Long demandeId = vars.get("demandeId") != null
                ? Long.valueOf(vars.get("demandeId").toString())
                : null;

        if (demandeId == null) {
            return false;
        }

        return demandeRepository.findById(demandeId)
                .map(demande -> {
                    if (demande.getEmploye() == null) {
                        return false;
                    }

                    Employe demandeur = demande.getEmploye();

                    Employe utilisateurConnecte = employeRepository.findByEmail(userEmail)
                            .orElse(null);

                    if (utilisateurConnecte == null) {
                        return false;
                    }

                    boolean sameId =
                            demandeur.getId() != null &&
                            utilisateurConnecte.getId() != null &&
                            Objects.equals(demandeur.getId(), utilisateurConnecte.getId());

                    boolean sameEmail =
                            demandeur.getEmail() != null &&
                            utilisateurConnecte.getEmail() != null &&
                            demandeur.getEmail().trim()
                                    .equalsIgnoreCase(utilisateurConnecte.getEmail().trim());

                    return sameId || sameEmail;
                })
                .orElse(false);

    } catch (Exception e) {
        log.warn(
                "Impossible de vérifier si la tâche {} appartient à {}: {}",
                task.getId(),
                userEmail,
                e.getMessage()
        );
        return false;
    }
}

private void verifierPasAutoTraitement(DemandeConge demande, String userEmail) {
    if (demande == null || demande.getEmploye() == null) {
        return;
    }

    if (userEmail == null || userEmail.isBlank()) {
        throw new BusinessException("Utilisateur connecté non identifié.");
    }

    Employe demandeur = demande.getEmploye();

    Employe utilisateurConnecte = employeRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException("Utilisateur connecté introuvable: " + userEmail));

    boolean memeEmployeParId =
            demandeur.getId() != null &&
            utilisateurConnecte.getId() != null &&
            Objects.equals(demandeur.getId(), utilisateurConnecte.getId());

    boolean memeEmployeParEmail =
            demandeur.getEmail() != null &&
            utilisateurConnecte.getEmail() != null &&
            demandeur.getEmail().trim()
                    .equalsIgnoreCase(utilisateurConnecte.getEmail().trim());

    if (memeEmployeParId || memeEmployeParEmail) {
        log.warn(
                "AUTO-TRAITEMENT BLOQUÉ: demandeId={}, demandeurEmail={}, userEmail={}, demandeurId={}, userId={}",
                demande.getId(),
                demandeur.getEmail(),
                userEmail,
                demandeur.getId(),
                utilisateurConnecte.getId()
        );

        throw new BusinessException("Vous ne pouvez pas traiter votre propre demande de congé.");
    }
}


    // ==================== DECISION MANAGER ====================

    @Transactional
    public void processManagerDecision(String taskId, Boolean approve, String comment, String managerEmail) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();

        if (task == null) {
            throw new BusinessException("Cette demande a déjà été traitée.");
        }

        if (!managerEmail.equalsIgnoreCase(task.getAssignee())) {
            throw new BusinessException("Vous n'êtes pas autorisé à traiter cette tâche");
        }

        String processInstanceId = task.getProcessInstanceId();

        Map<String, Object> vars = runtimeService.getVariables(processInstanceId);

        Long demandeId = vars.get("demandeId") != null
                ? Long.valueOf(vars.get("demandeId").toString())
                : null;

        if (demandeId == null) {
            throw new BusinessException("Demande introuvable dans les variables Camunda");
        }

        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new BusinessException("Demande introuvable: " + demandeId));

        verifierPasAutoTraitement(demande, managerEmail);

        if (demande.getManager() == null) {
            Employe employeManager = employeRepository.findByEmail(managerEmail).orElse(null);

            if (employeManager != null && "MANAGER".equalsIgnoreCase(employeManager.getRole())) {
                Manager manager = managerRepository.findByEmployeId(employeManager.getId())
                        .orElseThrow(() -> new BusinessException("Manager non trouvé pour " + managerEmail));

                demande.setManager(manager);
            }
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("managerApprouve", approve);

        if (comment != null && !comment.trim().isEmpty()) {
            variables.put("commentaireManager", comment);
        }

        taskService.complete(task.getId(), variables);

        if (Boolean.TRUE.equals(approve)) {
            demande.approuverParManager();

            Task nextTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .singleResult();

            if (nextTask != null) {
                demande.setCurrentTaskId(nextTask.getId());

                log.info(
                        "Nouvelle tâche RH enregistrée pour la demande {} : {}",
                        demande.getId(),
                        nextTask.getId()
                );
            } else {
                demande.setCurrentTaskId(null);

                log.warn(
                        "Aucune tâche RH active trouvée après approbation manager pour l'instance {}",
                        processInstanceId
                );
            }

        } else {
            demande.refuserParManager(comment != null ? comment : "Refusé par manager");
            demande.setCurrentTaskId(null);
        }

        demandeRepository.saveAndFlush(demande);

        log.info(
                "Décision manager: taskId={}, approve={}, manager={}, statutDemande={}",
                taskId,
                approve,
                managerEmail,
                demande.getStatut()
        );
    }

    // ==================== DECISION RH ====================

    @Transactional
    public void processRHDecision(String taskId, Boolean approve, String comment, String adminEmail) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();

        if (task == null) {
            throw new BusinessException("Cette demande a déjà été traitée.");
        }

        String assignee = task.getAssignee();

        if (assignee == null || assignee.isBlank()) {
            taskService.claim(task.getId(), adminEmail);
            log.info("Tâche RH {} claimée par {}", task.getId(), adminEmail);
        } else if (!adminEmail.equalsIgnoreCase(assignee)) {
            log.warn(
                    "Tâche RH {} assignée à {}, reprise par admin RH {}",
                    task.getId(),
                    assignee,
                    adminEmail
            );

            taskService.setAssignee(task.getId(), adminEmail);
        }

        String processInstanceId = task.getProcessInstanceId();

        Map<String, Object> vars = runtimeService.getVariables(processInstanceId);

        Long demandeId = vars.get("demandeId") != null
                ? Long.valueOf(vars.get("demandeId").toString())
                : null;

        if (demandeId == null) {
            throw new BusinessException("Demande introuvable dans les variables Camunda");
        }

        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new BusinessException("Demande introuvable: " + demandeId));

        verifierPasAutoTraitement(demande, adminEmail);

        administrateurRHRepository.findByEmail(adminEmail).ifPresent(demande::setAdminRh);

        if (Boolean.TRUE.equals(approve)) {
            demande.valider();
        } else {
            demande.refuserAvecMotif(comment != null ? comment : "Refusé par RH");
        }

        demande.setCurrentTaskId(null);

        Map<String, Object> variables = new HashMap<>();
        variables.put("rhApprouve", approve);

        if (comment != null && !comment.trim().isEmpty()) {
            variables.put("commentaireRH", comment);
        }

        taskService.complete(task.getId(), variables);

        demandeRepository.saveAndFlush(demande);

        log.info(
                "Décision RH: taskId={}, approve={}, admin={}, statutDemande={}",
                taskId,
                approve,
                adminEmail,
                demande.getStatut()
        );
    }

    // ==================== STATUS PROCESS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getProcessStatus(String processInstanceId) {
        Map<String, Object> status = new HashMap<>();

        status.put("processInstanceId", processInstanceId);

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        status.put("variables", variables);

        boolean isEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult() == null;

        status.put("isEnded", isEnded);

        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();

        List<Map<String, Object>> tasksInfo = activeTasks.stream().map(task -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("assignee", task.getAssignee());
            taskInfo.put("createTime", task.getCreateTime());
            return taskInfo;
        }).collect(Collectors.toList());

        status.put("activeTasks", tasksInfo);

        return status;
    }

    // ==================== ORPHAN REQUESTS ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrphanRequests() {
        return demandeRepository.findByProcessInstanceIdIsNull().stream().map(demande -> {
            Map<String, Object> map = new HashMap<>();

            map.put("id", demande.getId());
            map.put("employeId", demande.getEmploye() != null ? demande.getEmploye().getId() : null);
            map.put("employeNom", demande.getEmploye() != null ? demande.getEmploye().getNom() : null);
            map.put("employePrenom", demande.getEmploye() != null ? demande.getEmploye().getPrenom() : null);
            map.put("employeEmail", demande.getEmploye() != null ? demande.getEmploye().getEmail() : null);
            map.put("dateDebut", demande.getDateDebut() != null ? demande.getDateDebut().toString() : null);
            map.put("dateFin", demande.getDateFin() != null ? demande.getDateFin().toString() : null);
            map.put("joursOuvres", demande.getJoursOuvres());
            map.put("type", demande.getType());
            map.put("statut", demande.getStatut());
            map.put("dateDemande", demande.getDateDemande() != null ? demande.getDateDemande().toString() : null);

            return map;
        }).collect(Collectors.toList());
    }

    // ==================== DELETE PROCESS ====================

    @Transactional
    public void deleteProcessInstance(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return;
        }

        try {
            runtimeService.deleteProcessInstance(
                    processInstanceId,
                    "Annulé par l'utilisateur",
                    true,
                    true
            );

            log.info("Instance Camunda supprimée: {}", processInstanceId);

        } catch (ProcessEngineException e) {
            log.warn("Instance Camunda introuvable ou déjà supprimée: {}", e.getMessage());

        } catch (Exception e) {
            log.error(
                    "Erreur lors de la suppression de l'instance {}: {}",
                    processInstanceId,
                    e.getMessage()
            );
        }
    }



    @Transactional
public void envoyerDirectementVersRhSiManagerOuAdmin(DemandeConge demande, Employe demandeur) {
    if (demande == null || demandeur == null) {
        return;
    }

    if (!isRoleManagerOuAdmin(demandeur)) {
        return;
    }

    if (demande.getProcessInstanceId() == null || demande.getProcessInstanceId().isBlank()) {
        throw new BusinessException("Impossible d'envoyer directement vers RH: instance workflow manquante.");
    }

    Task activeTask = taskService.createTaskQuery()
            .processInstanceId(demande.getProcessInstanceId())
            .active()
            .singleResult();

    if (activeTask == null) {
        log.warn(
                "Aucune tâche active trouvée pour passage direct RH, demandeId={}, processInstanceId={}",
                demande.getId(),
                demande.getProcessInstanceId()
        );
        return;
    }

    /*
     * On complète automatiquement l'étape manager pour les demandeurs MANAGER/ADMIN.
     * La demande arrive ensuite directement à la tâche RH.
     */
    Map<String, Object> variables = new HashMap<>();
    variables.put("managerApprouve", true);
    variables.put("commentaireManager", "Passage automatique vers RH: demandeur manager/admin");

    log.info(
            "Passage automatique vers RH pour demandeId={}, demandeur={}, role={}, taskId={}",
            demande.getId(),
            demandeur.getEmail(),
            demandeur.getRole(),
            activeTask.getId()
    );

    taskService.complete(activeTask.getId(), variables);

    Task rhTask = taskService.createTaskQuery()
            .processInstanceId(demande.getProcessInstanceId())
            .active()
            .singleResult();

    demande.setStatut("EN_ATTENTE_RH");

    if (rhTask != null) {
        demande.setCurrentTaskId(rhTask.getId());
        log.info("Tâche RH créée pour demandeId={}, rhTaskId={}", demande.getId(), rhTask.getId());
    } else {
        demande.setCurrentTaskId(null);
        log.warn("Aucune tâche RH trouvée après passage automatique, demandeId={}", demande.getId());
    }

    demandeRepository.saveAndFlush(demande);
}

}