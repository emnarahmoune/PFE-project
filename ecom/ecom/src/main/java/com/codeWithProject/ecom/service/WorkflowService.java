package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.AdministrateurRH;
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

import java.util.*;
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
    private final AdministrateurRHRepository administrateurRHRepository; // Ajouté

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getManagerTasks(String managerEmail) {
        Employe employe = employeRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new BusinessException("Employé non trouvé: " + managerEmail));
        if (!"MANAGER".equalsIgnoreCase(employe.getRole())) {
            throw new BusinessException("Accès non autorisé: vous n'avez pas le rôle manager");
        }
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(managerEmail)
                .orderByTaskCreateTime().desc()
                .list();
        log.info("Manager {} a {} tâche(s) en attente", managerEmail, tasks.size());
        return tasks.stream().map(this::mapTaskToMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRHTasks(String adminEmail) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(adminEmail)
                .orderByTaskCreateTime().desc()
                .list();
        log.info("Admin {} a {} tâche(s) en attente", adminEmail, tasks.size());
        return tasks.stream().map(this::mapTaskToMap).collect(Collectors.toList());
    }

    private Map<String, Object> mapTaskToMap(Task task) {
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("taskId", task.getId());
        taskInfo.put("taskName", task.getName());
        taskInfo.put("createTime", task.getCreateTime());
        taskInfo.put("processInstanceId", task.getProcessInstanceId());
        Map<String, Object> vars = new HashMap<>();
        try {
            vars = runtimeService.getVariables(task.getProcessInstanceId());
        } catch (Exception e) {
            log.warn("Impossible de récupérer les variables: {}", e.getMessage());
        }
        taskInfo.put("employeId", vars.get("employeId"));
        taskInfo.put("nbJours", vars.get("nbJours"));
        taskInfo.put("demandeId", vars.get("demandeId"));
        taskInfo.put("montantConge", vars.get("montantConge"));

        Long demandeId = vars.get("demandeId") != null ? Long.valueOf(vars.get("demandeId").toString()) : null;
        if (demandeId != null) {
            demandeRepository.findById(demandeId).ifPresent(demande -> {
                taskInfo.put("dateDebut", demande.getDateDebut() != null ? demande.getDateDebut().toString() : null);
                taskInfo.put("dateFin", demande.getDateFin() != null ? demande.getDateFin().toString() : null);
                taskInfo.put("type", demande.getType());
                taskInfo.put("commentaire", demande.getCommentaire());
                if (demande.getEmploye() != null) {
                    taskInfo.put("employeNom", demande.getEmploye().getNom());
                    taskInfo.put("employePrenom", demande.getEmploye().getPrenom());
                    taskInfo.put("employeEmail", demande.getEmploye().getEmail());
                }
            });
        }
        return taskInfo;
    }

    @Transactional
    public void processManagerDecision(String taskId, Boolean approve, String comment, String managerEmail) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new BusinessException("Cette demande a déjà été traitée.");
        if (!managerEmail.equals(task.getAssignee()))
            throw new BusinessException("Vous n'êtes pas autorisé à traiter cette tâche");

        String processInstanceId = task.getProcessInstanceId();
        Map<String, Object> vars = runtimeService.getVariables(processInstanceId);
        Long demandeId = vars.get("demandeId") != null ? Long.valueOf(vars.get("demandeId").toString()) : null;

        if (demandeId != null) {
            DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
            if (demande != null) {
                if (!"EN_ATTENTE".equals(demande.getStatut()))
                    throw new BusinessException("Seules les demandes EN_ATTENTE peuvent être validées/refusées");

                Employe employeManager = employeRepository.findByEmail(managerEmail).orElse(null);
                if (employeManager != null && "MANAGER".equalsIgnoreCase(employeManager.getRole())) {
                    Manager manager = managerRepository.findByEmployeId(employeManager.getId())
                            .orElseThrow(() -> new BusinessException("Manager non trouvé pour " + managerEmail));
                    demande.setManager(manager);
                    demandeRepository.save(demande);
                    log.info("✅ Manager {} enregistré pour la demande {}", managerEmail, demandeId);
                }
            }
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("managerApprouve", approve);
        if (comment != null && !comment.trim().isEmpty()) variables.put("commentaireManager", comment);
        taskService.complete(taskId, variables);
        log.info("Décision manager: taskId={}, approve={}, manager={}", taskId, approve, managerEmail);
    }

    @Transactional
    public void processRHDecision(String taskId, Boolean approve, String comment, String adminEmail) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new BusinessException("Cette demande a déjà été traitée.");
        if (!adminEmail.equals(task.getAssignee()))
            throw new BusinessException("Vous n'êtes pas autorisé à traiter cette tâche");

        String processInstanceId = task.getProcessInstanceId();
        Map<String, Object> vars = runtimeService.getVariables(processInstanceId);
        Long demandeId = vars.get("demandeId") != null ? Long.valueOf(vars.get("demandeId").toString()) : null;

        // ✅ Enregistrer l'admin RH dans la demande
        if (demandeId != null) {
            DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
            if (demande != null) {
                if (!"EN_ATTENTE".equals(demande.getStatut()))
                    throw new BusinessException("Seules les demandes EN_ATTENTE peuvent être validées/refusées");

                administrateurRHRepository.findByEmail(adminEmail).ifPresent(demande::setAdminRh);
                demandeRepository.save(demande);
                log.info("✅ Admin RH {} enregistré pour la demande {}", adminEmail, demandeId);
            }
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("rhApprouve", approve);
        if (comment != null && !comment.trim().isEmpty()) variables.put("commentaireRH", comment);
        taskService.complete(taskId, variables);
        log.info("Décision RH: taskId={}, approve={}, admin={}", taskId, approve, adminEmail);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProcessStatus(String processInstanceId) {
        Map<String, Object> status = new HashMap<>();
        status.put("processInstanceId", processInstanceId);
        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        status.put("variables", variables);
        boolean isEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult() == null;
        status.put("isEnded", isEnded);
        List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        List<Map<String, Object>> tasksInfo = activeTasks.stream().map(task -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("assignee", task.getAssignee());
            taskInfo.put("createTime", task.getCreateTime());
            return taskInfo;
        }).collect(Collectors.toList());
        status.put("activeTasks", tasksInfo);
        Long demandeId = variables.get("demandeId") != null ? Long.valueOf(variables.get("demandeId").toString()) : null;
        if (demandeId != null) {
            demandeRepository.findById(demandeId).ifPresent(demande -> {
                status.put("demande", Map.of(
                        "id", demande.getId(),
                        "statut", demande.getStatut(),
                        "dateDebut", demande.getDateDebut() != null ? demande.getDateDebut().toString() : null,
                        "dateFin", demande.getDateFin() != null ? demande.getDateFin().toString() : null,
                        "type", demande.getType(),
                        "joursOuvres", demande.getJoursOuvres()
                ));
            });
        }
        return status;
    }

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

    @Transactional
    public void deleteProcessInstance(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) return;
        try {
            runtimeService.deleteProcessInstance(processInstanceId, "Annulé par l'utilisateur", true, true);
            log.info("Instance Camunda supprimée: {}", processInstanceId);
        } catch (ProcessEngineException e) {
            log.warn("Instance Camunda introuvable ou déjà supprimée: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'instance {}: {}", processInstanceId, e.getMessage());
        }
    }
}