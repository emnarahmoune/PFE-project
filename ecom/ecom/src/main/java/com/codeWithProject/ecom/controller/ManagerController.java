package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.ManagerService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.ManagerDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Managers", description = "API de gestion des managers")
public class ManagerController {

    private final ManagerService managerService;
    private final WorkflowService workflowService;
    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final DemandeCongeService demandeCongeService;

    @GetMapping
    @Operation(summary = "Liste tous les managers")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getAllManagers() {
        log.info("GET /api/managers");
        List<ManagerDTO> managers = managerService.findAll();
        return ResponseEntity.ok(ApiResponse.success(managers, "Managers récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des managers")
    public ResponseEntity<ApiResponse<Page<ManagerDTO>>> getAllManagersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("GET /api/managers/paged - page: {}, size: {}", page, size);
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ManagerDTO> managers = managerService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(managers, "Managers récupérés avec succès"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un manager par son ID")
    public ResponseEntity<ApiResponse<ManagerDTO>> getManagerById(@PathVariable Long id) {
        log.info("GET /api/managers/{}", id);
        return managerService.findById(id)
                .map(manager -> ResponseEntity.ok(ApiResponse.success(manager, "Manager trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère un manager par l'ID de l'employé associé")
    public ResponseEntity<ApiResponse<ManagerDTO>> getManagerByEmployeId(@PathVariable Long employeId) {
        log.info("GET /api/managers/employe/{}", employeId);
        return managerService.findByEmployeId(employeId)
                .map(manager -> ResponseEntity.ok(ApiResponse.success(manager, "Manager trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departement/{departement}")
    @Operation(summary = "Récupère les managers par département")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getManagersByDepartement(@PathVariable String departement) {
        log.info("GET /api/managers/departement/{}", departement);
        List<ManagerDTO> managers = managerService.findByDepartement(departement);
        return ResponseEntity.ok(ApiResponse.success(managers, "Managers par département récupérés"));
    }

    @GetMapping("/actifs")
    @Operation(summary = "Récupère les managers actifs")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getManagersActifs() {
        log.info("GET /api/managers/actifs");
        List<ManagerDTO> actifs = managerService.findManagersActifs();
        return ResponseEntity.ok(ApiResponse.success(actifs, "Managers actifs récupérés"));
    }

    @GetMapping("/sans-equipe")
    @Operation(summary = "Récupère les managers sans équipe")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getManagersSansEquipe() {
        log.info("GET /api/managers/sans-equipe");
        List<ManagerDTO> sansEquipe = managerService.findManagersSansEquipe();
        return ResponseEntity.ok(ApiResponse.success(sansEquipe, "Managers sans équipe récupérés"));
    }

    @PostMapping
    @Operation(summary = "Crée un nouveau manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> createManager(@Valid @RequestBody ManagerDTO dto) {
        log.info("POST /api/managers - Création manager pour département: {}", dto.getDepartement());
        ManagerDTO created = managerService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Manager créé avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> updateManager(@PathVariable Long id, @Valid @RequestBody ManagerDTO dto) {
        log.info("PUT /api/managers/{}", id);
        ManagerDTO updated = managerService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Manager mis à jour avec succès"));
    }

    @PatchMapping("/{id}/activer")
    @Operation(summary = "Active un manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> activerManager(@PathVariable Long id) {
        log.info("PATCH /api/managers/{}/activer", id);
        ManagerDTO active = managerService.activer(id);
        return ResponseEntity.ok(ApiResponse.success(active, "Manager activé avec succès"));
    }

    @PatchMapping("/{id}/desactiver")
    @Operation(summary = "Désactive un manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> desactiverManager(@PathVariable Long id) {
        log.info("PATCH /api/managers/{}/desactiver", id);
        ManagerDTO desactive = managerService.desactiver(id);
        return ResponseEntity.ok(ApiResponse.success(desactive, "Manager désactivé avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un manager")
    public ResponseEntity<ApiResponse<Void>> deleteManager(@PathVariable Long id) {
        log.info("DELETE /api/managers/{}", id);
        managerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Manager supprimé avec succès"));
    }

    @PostMapping("/{managerId}/employes/{employeId}")
    @Operation(summary = "Ajoute un employé à l'équipe du manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> ajouterEmploye(@PathVariable Long managerId, @PathVariable Long employeId) {
        log.info("POST /api/managers/{}/employes/{}", managerId, employeId);
        ManagerDTO updated = managerService.ajouterEmploye(managerId, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employé ajouté à l'équipe avec succès"));
    }

    @DeleteMapping("/{managerId}/employes/{employeId}")
    @Operation(summary = "Retire un employé de l'équipe du manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> retirerEmploye(@PathVariable Long managerId, @PathVariable Long employeId) {
        log.info("DELETE /api/managers/{}/employes/{}", managerId, employeId);
        ManagerDTO updated = managerService.retirerEmploye(managerId, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employé retiré de l'équipe avec succès"));
    }

    @GetMapping("/{id}/rapport-equipe")
    @Operation(summary = "Génère un rapport d'équipe pour un manager")
    public ResponseEntity<ApiResponse<String>> getRapportEquipe(@PathVariable Long id) {
        log.info("GET /api/managers/{}/rapport-equipe", id);
        String rapport = managerService.genererRapportEquipe(id);
        return ResponseEntity.ok(ApiResponse.success(rapport, "Rapport d'équipe généré"));
    }

    @GetMapping("/stats/globales")
    @Operation(summary = "Récupère les statistiques globales des managers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getManagersStats() {
        log.info("GET /api/managers/stats/globales");
        Map<String, Object> stats = managerService.getManagersStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques des managers récupérées"));
    }

    @GetMapping("/stats/departement")
    @Operation(summary = "Compte les managers par département")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countByDepartement() {
        log.info("GET /api/managers/stats/departement");
        Map<String, Long> stats = managerService.countByDepartement();
        return ResponseEntity.ok(ApiResponse.success(stats, "Managers par département comptés"));
    }

    @GetMapping("/stats/anciennete-moyenne")
    @Operation(summary = "Calcule l'ancienneté moyenne des managers")
    public ResponseEntity<ApiResponse<Double>> getAncienneteMoyenne() {
        log.info("GET /api/managers/stats/anciennete-moyenne");
        Double moyenne = managerService.calculerAncienneteMoyenne();
        return ResponseEntity.ok(ApiResponse.success(moyenne, "Ancienneté moyenne calculée"));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des managers par mot-clé")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> searchManagers(@RequestParam String keyword) {
        log.info("GET /api/managers/search?keyword={}", keyword);
        List<ManagerDTO> result = managerService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }

    // ===== WORKFLOW MANAGER =====

    @GetMapping("/mon-equipe")
    @Operation(summary = "Récupère l'équipe du manager connecté")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Employe>>> getMonEquipe(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère son équipe", email);
        List<Employe> equipe = employeRepository.findByManagerEmail(email);
        return ResponseEntity.ok(ApiResponse.success(equipe, "Équipe récupérée avec succès"));
    }

    @GetMapping("/demandes-conge")
    @Operation(summary = "Récupère les demandes de congé de l'équipe")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDemandesConge(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère les demandes de congé", email);
        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
        return ResponseEntity.ok(ApiResponse.success(tasks, "Demandes récupérées avec succès"));
    }

    @GetMapping("/stats-dashboard")
    @Operation(summary = "Récupère les statistiques pour le dashboard manager")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("Manager {} récupère ses statistiques", email);

        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
        stats.put("demandesEnAttente", tasks.size());

        List<Employe> equipe = employeRepository.findByManagerEmail(email);
        stats.put("nbEmployes", equipe.size());

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

        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées avec succès"));
    }

    @PostMapping("/approuver-demande")
    @Operation(summary = "Approuve une demande de congé")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> approuverDemande(@RequestBody Map<String, Object> decision, @AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        String taskId = (String) decision.get("taskId");
        String commentaire = (String) decision.get("commentaire");
        log.info("Manager {} approuve la tâche {}", email, taskId);
        workflowService.processManagerDecision(taskId, true, commentaire, email);
        return ResponseEntity.ok(ApiResponse.success("Demande approuvée avec succès"));
    }

    @PostMapping("/refuser-demande")
    @Operation(summary = "Refuse une demande de congé")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> refuserDemande(@RequestBody Map<String, Object> decision, @AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        String taskId = (String) decision.get("taskId");
        String motif = (String) decision.get("motif");
        log.info("Manager {} refuse la tâche {} avec motif: {}", email, taskId, motif);
        workflowService.processManagerDecision(taskId, false, motif, email);
        return ResponseEntity.ok(ApiResponse.success("Demande refusée avec succès"));
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
    @GetMapping("/calendar-events")
    @Operation(summary = "Récupère les événements calendrier des employés de l'équipe du manager connecté")
    @PreAuthorize("hasRole('manager') or hasRole('MANAGER')")
    public ResponseEntity<List<CalendarEventDTO>> getCalendarEventsForManager(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("GET /api/managers/calendar-events pour manager : {}", email);
        List<CalendarEventDTO> events = demandeCongeService.getCalendarEventsForManager(email);
        return ResponseEntity.ok(events);
    }
}