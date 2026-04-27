package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
public class ManagerWorkflowController {

    private final WorkflowService workflowService;
    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère ses statistiques", email);

        Map<String, Object> stats = new HashMap<>();
        List<Employe> equipe = employeRepository.findByManagerEmail(email);
        stats.put("employes", equipe.size());

        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
        stats.put("congesEnAttente", tasks.size());

        long demandesApprouvees = demandeCongeRepository.countByStatut("APPROUVE");
        long demandesRefusees = demandeCongeRepository.countByStatut("REFUSE");
        stats.put("demandesApprouvees", demandesApprouvees);
        stats.put("demandesRefusees", demandesRefusees);

        if (demandesApprouvees + demandesRefusees > 0) {
            double tauxApprobation = (double) demandesApprouvees / (demandesApprouvees + demandesRefusees) * 100;
            stats.put("tauxApprobation", Math.round(tauxApprobation));
        } else {
            stats.put("tauxApprobation", 0);
        }

        stats.put("absenteisme", 0.0);
        stats.put("turnover", 0.0);

        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées"));
    }

    @GetMapping("/equipe")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Employe>>> getEquipe(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère son équipe", email);
        List<Employe> equipe = employeRepository.findByManagerEmail(email);
        return ResponseEntity.ok(ApiResponse.success(equipe, "Équipe récupérée"));
    }

    @GetMapping("/conges")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConges(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère les demandes de congé de son équipe", email);
        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
        return ResponseEntity.ok(ApiResponse.success(tasks, "Demandes récupérées"));
    }

    @GetMapping("/alertes")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> getAlertes(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère ses alertes", email);

        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
        List<String> alertes = new java.util.ArrayList<>();

        if (!tasks.isEmpty()) {
            alertes.add(tasks.size() + " demande(s) de congé en attente de validation");
        }

        List<Employe> equipe = employeRepository.findByManagerEmail(email);
        long employesSoldeFaible = equipe.stream()
                .filter(e -> e.getSoldeConges() != null && e.getSoldeConges() < 5)
                .count();

        if (employesSoldeFaible > 0) {
            alertes.add(employesSoldeFaible + " employé(s) ont un solde de congés faible (<5 jours)");
        }

        if (alertes.isEmpty()) {
            alertes.add("Aucune alerte pour le moment");
        }

        return ResponseEntity.ok(ApiResponse.success(alertes, "Alertes récupérées"));
    }

    // ========== MÉTHODES AJOUTÉES POUR LES DÉCISIONS MANAGER ==========

    @PostMapping("/approuver-demande")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> approuverDemande(
            @RequestBody Map<String, Object> decision,
            @AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        String taskId = (String) decision.get("taskId");
        String commentaire = (String) decision.get("commentaire");
        log.info("Manager {} approuve la tâche {}", email, taskId);
        workflowService.processManagerDecision(taskId, true, commentaire, email);
        return ResponseEntity.ok(ApiResponse.success("Demande approuvée avec succès"));
    }

    @PostMapping("/refuser-demande")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> refuserDemande(
            @RequestBody Map<String, Object> decision,
            @AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        String taskId = (String) decision.get("taskId");
        String motif = (String) decision.get("motif");
        log.info("Manager {} refuse la tâche {} avec motif: {}", email, taskId, motif);
        workflowService.processManagerDecision(taskId, false, motif, email);
        return ResponseEntity.ok(ApiResponse.success("Demande refusée avec succès"));
    }

    // ================================================================

    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
}