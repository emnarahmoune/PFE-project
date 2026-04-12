package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.controller.dto.ApiResponse;
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

    @GetMapping("/manager/tasks")
    @Operation(summary = "Tâches du manager à approuver")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getManagerTasks(
            @AuthenticationPrincipal Jwt jwt) {

        String email = extractEmail(jwt);
        log.info("Manager {} récupère ses tâches", email);

        try {
            List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
            return ResponseEntity.ok(ApiResponse.success(tasks, "Tâches récupérées"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
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
            log.error("Erreur technique: {}", e.getMessage());
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
            return ResponseEntity.ok(ApiResponse.success(tasks, "Tâches RH récupérées"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
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
            log.error("Erreur: {}", e.getMessage());
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
            log.error("Erreur: {}", e.getMessage());
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
        return ResponseEntity.ok(ApiResponse.success(orphanRequests, "Demandes orphelines récupérées"));
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
}