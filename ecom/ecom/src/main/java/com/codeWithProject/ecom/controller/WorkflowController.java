package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.exception.BusinessException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Congés", description = "Gestion du workflow Camunda")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;

    @GetMapping("/manager/tasks")
    @Operation(summary = "Tâches du manager à approuver")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getManagerTasks(
            @AuthenticationPrincipal Jwt jwt) {

        String email = extractEmail(jwt);
        log.info("Manager {} récupère ses tâches", email);

        try {
            List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);

            safeEnrichTasksWithEmployeePhotos(tasks);

            return ResponseEntity.ok(ApiResponse.success(tasks, "Tâches récupérées"));
        } catch (Exception e) {
            log.error("Erreur récupération tâches manager", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/manager/decide")
    @Operation(summary = "Décision du manager (approuver/refuser)")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> managerDecision(
            @RequestBody Map<String, Object> decision,
            @AuthenticationPrincipal Jwt jwt) {

        String email = extractEmail(jwt);
        String taskId = (String) decision.get("taskId");
        Boolean approve = (Boolean) decision.get("approve");
        String comment = (String) decision.get("comment");

        log.info("Manager {} décide: taskId={}, approve={}", email, taskId, approve);

        try {
            workflowService.processManagerDecision(taskId, approve, comment, email);

            return ResponseEntity.ok(ApiResponse.success("Décision enregistrée"));
        } catch (BusinessException e) {
            log.warn("Erreur métier: {}", e.getMessage());

            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique décision manager", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/rh/tasks")
    @Operation(summary = "Tâches RH à approuver (demandes > 10 jours)")
    @PreAuthorize("hasRole('admin_rh') or hasRole('ADMIN_RH') or hasRole('admin')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRHTasks(
            @AuthenticationPrincipal Jwt jwt) {

        String email = extractEmail(jwt);
        log.info("Admin RH {} récupère ses tâches", email);

        try {
            List<Map<String, Object>> tasks = workflowService.getRHTasks(email);

            safeEnrichTasksWithEmployeePhotos(tasks);

            return ResponseEntity.ok(ApiResponse.success(tasks, "Tâches RH récupérées"));
        } catch (Exception e) {
            log.error("Erreur récupération tâches RH", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/rh/decide")
    @Operation(summary = "Décision du RH")
    @PreAuthorize("hasRole('admin_rh') or hasRole('ADMIN_RH') or hasRole('admin')")
    public ResponseEntity<ApiResponse<String>> rhDecision(
            @RequestBody Map<String, Object> decision,
            @AuthenticationPrincipal Jwt jwt) {

        String email = extractEmail(jwt);
        String taskId = (String) decision.get("taskId");
        Boolean approve = (Boolean) decision.get("approve");
        String comment = (String) decision.get("comment");

        log.info("Admin RH {} décide: taskId={}, approve={}", email, taskId, approve);

        try {
            workflowService.processRHDecision(taskId, approve, comment, email);

            return ResponseEntity.ok(ApiResponse.success("Décision RH enregistrée"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur décision RH", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/instance/{processInstanceId}")
    @Operation(summary = "Suivre l'avancement d'une demande")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessStatus(
            @PathVariable String processInstanceId,
            @AuthenticationPrincipal Jwt jwt) {

        String email = extractEmail(jwt);
        log.info("Utilisateur {} consulte l'instance {}", email, processInstanceId);

        try {
            Map<String, Object> status = workflowService.getProcessStatus(processInstanceId);

            return ResponseEntity.ok(ApiResponse.success(status, "Statut récupéré"));
        } catch (Exception e) {
            log.error("Erreur récupération statut process", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/orphan-requests")
    @Operation(summary = "Demandes orphelines (sans instance Camunda)")
    @PreAuthorize("hasRole('admin_rh') or hasRole('ADMIN_RH') or hasRole('admin')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrphanRequests() {
        log.info("Récupération des demandes orphelines");

        List<Map<String, Object>> orphanRequests = workflowService.getOrphanRequests();

        safeEnrichTasksWithEmployeePhotos(orphanRequests);

        return ResponseEntity.ok(
                ApiResponse.success(orphanRequests, "Demandes orphelines récupérées")
        );
    }

    // ============================================================
    // PHOTO HELPERS
    // ============================================================

    private void safeEnrichTasksWithEmployeePhotos(List<Map<String, Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (Map<String, Object> task : tasks) {
            try {
                enrichOneTaskWithPhoto(task);
            } catch (Exception e) {
                log.warn("Photo non ajoutée pour task = {}. Erreur = {}", task, e.getMessage());

                if (task != null) {
                    task.putIfAbsent("photoUrl", null);
                    task.putIfAbsent("employePhotoProfil", null);
                    task.putIfAbsent("employePhotoUrl", null);
                }
            }
        }
    }

    private void enrichOneTaskWithPhoto(Map<String, Object> task) {
    if (task == null) {
        return;
    }

    enrichTaskWithDemandeData(task);

    log.info("TASK AVANT PHOTO = {}", task);

    Employe employe = findEmployeFromTask(task);

    if (employe == null) {
        log.warn("Aucun employé trouvé pour task = {}", task);

        task.put("photoUrl", null);
        task.put("employePhotoProfil", null);
        task.put("employePhotoUrl", null);
        return;
    }

    String photo = employe.getPhotoUrl();

    task.put("photoUrl", photo);
    task.put("employePhotoProfil", photo);
    task.put("employePhotoUrl", photo);

    task.put("employeId", employe.getId());
    task.put("employeNom", employe.getNom());
    task.put("employePrenom", employe.getPrenom());
    task.put("employeEmail", employe.getEmail());

    log.info(
            "PHOTO TROUVEE POUR EMPLOYE {} {} => {}",
            employe.getPrenom(),
            employe.getNom(),
            photo
    );

    log.info("TASK APRES PHOTO = {}", task);
}

    private Employe findEmployeFromTask(Map<String, Object> task) {

        /*
         * 1) D'abord employeId.
         * Ton log montre que la tâche contient déjà employeId=4.
         * C'est le chemin le plus sûr et ça évite LazyInitializationException.
         */
        Long employeId = extractLong(task.get("employeId"));

        if (employeId == null) {
            employeId = extractLong(task.get("employeeId"));
        }

        if (employeId == null) {
            employeId = extractLong(task.get("idEmploye"));
        }

        if (employeId != null) {
            Employe employe = employeRepository.findById(employeId).orElse(null);

            if (employe != null) {
                return employe;
            }
        }

        /*
         * 2) Ensuite email.
         */
        String email = extractString(task.get("employeEmail"));

        if (email == null) {
            email = extractString(task.get("employeeEmail"));
        }

        if (email == null) {
            email = extractString(task.get("email"));
        }

        if (email != null) {
            Employe employe = employeRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                    .orElse(null);

            if (employe != null) {
                return employe;
            }
        }

        /*
         * 3) Ensuite demandeId.
         * Important : on ne retourne jamais directement demande.getEmploye().
         * On récupère seulement son id, puis on recharge Employe via employeRepository.
         */
        Long demandeId = extractLong(task.get("demandeId"));

        if (demandeId == null) {
            demandeId = extractLong(task.get("demandeCongeId"));
        }

        if (demandeId == null) {
            demandeId = extractLong(task.get("congeId"));
        }

        if (demandeId == null) {
            demandeId = extractLong(task.get("idDemande"));
        }

        if (demandeId != null) {
            DemandeConge demande = demandeCongeRepository.findById(demandeId).orElse(null);

            if (demande != null && demande.getEmploye() != null && demande.getEmploye().getId() != null) {
                Long idFromDemande = demande.getEmploye().getId();

                return employeRepository.findById(idFromDemande).orElse(null);
            }
        }

        /*
         * 4) Ensuite processInstanceId.
         * Pareil : ne pas retourner directement demande.getEmploye().
         */
        String processInstanceId = extractString(task.get("processInstanceId"));

        if (processInstanceId == null) {
            processInstanceId = extractString(task.get("processId"));
        }

        if (processInstanceId == null) {
            processInstanceId = extractString(task.get("instanceId"));
        }

        if (processInstanceId != null) {
            final String finalProcessInstanceId = processInstanceId;

            DemandeConge demande = demandeCongeRepository.findAll()
                    .stream()
                    .filter(d -> d.getProcessInstanceId() != null)
                    .filter(d -> d.getProcessInstanceId().equals(finalProcessInstanceId))
                    .findFirst()
                    .orElse(null);

            if (demande != null && demande.getEmploye() != null && demande.getEmploye().getId() != null) {
                Long idFromDemande = demande.getEmploye().getId();

                return employeRepository.findById(idFromDemande).orElse(null);
            }
        }

        /*
         * 5) Dernier fallback : prénom + nom.
         */
        String prenom = extractString(task.get("employePrenom"));
        String nom = extractString(task.get("employeNom"));

        if (prenom != null && nom != null) {
            final String finalPrenom = prenom;
            final String finalNom = nom;

            return employeRepository.findAll()
                    .stream()
                    .filter(e -> e.getPrenom() != null && e.getNom() != null)
                    .filter(e -> e.getPrenom().equalsIgnoreCase(finalPrenom))
                    .filter(e -> e.getNom().equalsIgnoreCase(finalNom))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private Long extractLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String extractString(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isBlank() ? null : text;
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            email = jwt.getSubject();
        }

        return email != null ? email.trim().toLowerCase() : null;
 
    }




   private void enrichTaskWithDemandeData(Map<String, Object> task) {
    if (task == null) {
        return;
    }

    DemandeConge demande = findDemandeFromTask(task);

    if (demande == null) {
        task.putIfAbsent("urgente", false);
        task.putIfAbsent("urgent", false);
        task.putIfAbsent("isUrgent", false);
        return;
    }

    boolean urgente = Boolean.TRUE.equals(demande.getUrgente());

    task.put("urgente", urgente);
    task.put("urgent", urgente);
    task.put("isUrgent", urgente);

    task.putIfAbsent("demandeId", demande.getId());
    task.putIfAbsent("dateDebut", demande.getDateDebut());
    task.putIfAbsent("dateFin", demande.getDateFin());
    task.putIfAbsent("type", demande.getType());
    task.putIfAbsent("typeConge", demande.getType());
    task.putIfAbsent("commentaire", demande.getCommentaire());
    task.putIfAbsent("statut", demande.getStatut());
    task.putIfAbsent("nbJours", demande.getJoursOuvres());
    task.putIfAbsent("joursOuvres", demande.getJoursOuvres());

    /*
     * IMPORTANT :
     * Ne pas faire demande.getEmploye().getNom(), getPrenom(), getEmail ici.
     * Ça peut casser à cause du lazy loading.
     * On ne force que demandeId, puis findEmployeFromTask() recharge l'employé proprement
     * avec employeRepository.
     */
}

private DemandeConge findDemandeFromTask(Map<String, Object> task) {
    Long demandeId = extractLong(task.get("demandeId"));

    if (demandeId == null) {
        demandeId = extractLong(task.get("demandeCongeId"));
    }

    if (demandeId == null) {
        demandeId = extractLong(task.get("congeId"));
    }

    if (demandeId == null) {
        demandeId = extractLong(task.get("idDemande"));
    }

    if (demandeId != null) {
        return demandeCongeRepository.findById(demandeId).orElse(null);
    }

    String processInstanceId = extractString(task.get("processInstanceId"));

    if (processInstanceId == null) {
        processInstanceId = extractString(task.get("processId"));
    }

    if (processInstanceId == null) {
        processInstanceId = extractString(task.get("instanceId"));
    }

    if (processInstanceId != null) {
        final String finalProcessInstanceId = processInstanceId;

        return demandeCongeRepository.findAll()
                .stream()
                .filter(d -> d.getProcessInstanceId() != null)
                .filter(d -> d.getProcessInstanceId().equals(finalProcessInstanceId))
                .findFirst()
                .orElse(null);
    }

    return null;
}
}