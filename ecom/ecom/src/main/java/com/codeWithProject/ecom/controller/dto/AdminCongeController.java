package com.codeWithProject.ecom.controller.dto;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.AdminCongeService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusDetailsDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusManagerDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/conges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin RH - Gestion des congés", description = "API pour l'administration des demandes de congé")
@PreAuthorize("hasRole('ADMIN_RH') or hasRole('admin')")
public class AdminCongeController {

    private final AdminCongeService adminCongeService;
    private final WorkflowService workflowService;
    private final DemandeCongeRepository demandeCongeRepository;
    private final ObjectMapper objectMapper;

    // ✅ AJOUTÉ POUR ENRICHIR LES TÂCHES RH AVEC photo_url
    private final EmployeRepository employeRepository;

    @GetMapping("/a-valider")
    @Operation(summary = "Récupère les demandes en attente (tâches Camunda)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDemandesAValider(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("GET /api/admin/conges/a-valider - Récupération des tâches RH");

        String adminEmail = extractEmail(jwt);

        try {
            List<Map<String, Object>> tasks = workflowService.getRHTasks(adminEmail);

            // ✅ IMPORTANT : workflowService.getRHTasks() retourne des Map sans photo.
            // Ici on ajoute photoUrl / employePhotoProfil / employePhotoUrl à chaque tâche.
            enrichTasksWithEmployeePhotos(tasks);

            return ResponseEntity.ok(ApiResponse.success(tasks, "Tâches récupérées avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ Événements pour le calendrier FullCalendar
    @GetMapping("/calendar-events")
    @Operation(summary = "Récupère les événements pour le calendrier")
    public ResponseEntity<ApiResponse<List<CalendarEventDTO>>> getCalendarEvents() {
        log.info("GET /api/admin/conges/calendar-events");

        try {
            List<CalendarEventDTO> events = adminCongeService.getAllCalendarEvents();

            return ResponseEntity.ok(ApiResponse.success(events, "Événements récupérés"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ Détails complets d'une demande refusée
    @GetMapping("/demandes/{id}/refus-details")
    @Operation(summary = "Détails d'une demande refusée (avec heure de soumission et heure de décision manager)")
    public ResponseEntity<ApiResponse<DemandeRefusDetailsDTO>> getRefusDetails(@PathVariable Long id) {
        log.info("GET /api/admin/conges/demandes/{}/refus-details", id);

        try {
            DemandeRefusDetailsDTO details = adminCongeService.getRefusDetails(id);

            return ResponseEntity.ok(ApiResponse.success(details, "Détails récupérés"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
@GetMapping("/refus-manager")
@Operation(summary = "Récupère les demandes refusées par les managers")
public ResponseEntity<ApiResponse<List<DemandeRefusManagerDTO>>> getDemandesRefuseesParManager() {
    log.info("GET /api/admin/conges/refus-manager");

    try {
        List<DemandeRefusManagerDTO> demandes = adminCongeService.getDemandesRefuseesParManager();

        return ResponseEntity.ok(ApiResponse.success(demandes, "Refus manager récupérés"));
    } catch (Exception e) {
        log.error("Erreur refus manager", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
    }
}

    private List<Map<String, Object>> enrichRefusManagerWithEmployeePhotos(List<DemandeRefusManagerDTO> demandes) {
    List<Map<String, Object>> result = new ArrayList<>();

    if (demandes == null || demandes.isEmpty()) {
        return result;
    }

    for (DemandeRefusManagerDTO demandeDto : demandes) {
        if (demandeDto == null) {
            continue;
        }

        Map<String, Object> item = objectMapper.convertValue(demandeDto, Map.class);

        Long demandeId = extractLong(item.get("id"));
        String photo = null;

        if (demandeId != null) {
            DemandeConge demande = demandeCongeRepository.findById(demandeId).orElse(null);

            if (demande != null && demande.getEmploye() != null) {
                photo = demande.getEmploye().getPhotoUrl();

                item.putIfAbsent("employeId", demande.getEmploye().getId());
                item.putIfAbsent("employeNom", demande.getEmploye().getNom());
                item.putIfAbsent("employePrenom", demande.getEmploye().getPrenom());
                item.putIfAbsent("employeEmail", demande.getEmploye().getEmail());
                item.putIfAbsent("employeDepartement", demande.getEmploye().getDepartement());
            }
        }

        item.put("photoUrl", photo);
        item.put("employePhotoProfil", photo);
        item.put("employePhotoUrl", photo);

        result.add(item);
    }

    return result;
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
        return Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
        return null;
    }
}
    @GetMapping("/all")
    @Operation(summary = "Récupère toutes les demandes de congé")
    public ResponseEntity<ApiResponse<List<DemandeCongeAdminDTO>>> getAllDemandes() {
        log.info("GET /api/admin/conges/all - Récupération de toutes les demandes");

        try {
            List<DemandeCongeAdminDTO> demandes = adminCongeService.getAllDemandes();

            return ResponseEntity.ok(ApiResponse.success(demandes, "Toutes les demandes récupérées"));
        } catch (Exception e) {
            log.error("Erreur complète /api/admin/conges/all", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur chargement historique congés: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère une demande par son ID")
    public ResponseEntity<ApiResponse<DemandeCongeAdminDTO>> getDemandeById(@PathVariable Long id) {
        log.info("GET /api/admin/conges/{}", id);

        try {
            DemandeCongeAdminDTO demande = adminCongeService.getDemandeById(id);

            return ResponseEntity.ok(ApiResponse.success(demande, "Demande trouvée"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/valider")
    @Operation(summary = "Approuve une demande de congé")
    public ResponseEntity<ApiResponse<Void>> validerDemande(
            @PathVariable Long id,
            @RequestParam(required = false) String commentaire,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /api/admin/conges/{}/valider - Approbation demande", id);

        String adminEmail = extractEmail(jwt);

        try {
            adminCongeService.validerDemande(id, commentaire, adminEmail);

            return ResponseEntity.ok(ApiResponse.success(null, "Demande approuvée avec succès"));
        } catch (BusinessException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de l'approbation: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de l'approbation: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/refuser")
    @Operation(summary = "Refuse une demande de congé avec un motif")
    public ResponseEntity<ApiResponse<Void>> refuserDemande(
            @PathVariable Long id,
            @RequestParam String motif,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /api/admin/conges/{}/refuser - Refus demande, motif: {}", id, motif);

        String adminEmail = extractEmail(jwt);

        try {
            adminCongeService.refuserDemande(id, motif, adminEmail);

            return ResponseEntity.ok(ApiResponse.success(null, "Demande refusée avec succès"));
        } catch (BusinessException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors du refus: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du refus: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/statut")
    @Operation(summary = "Statistiques des demandes par statut")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        log.info("GET /api/admin/conges/stats/statut");

        try {
            Map<String, Long> stats = adminCongeService.getStatsByStatut();

            return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/orphan-requests")
    @Operation(summary = "Demandes orphelines (sans instance Camunda)")
    public ResponseEntity<ApiResponse<List<DemandeCongeAdminDTO>>> getOrphanRequests() {
        log.info("GET /api/admin/conges/orphan-requests - Récupération des demandes orphelines");

        try {
            List<DemandeCongeAdminDTO> demandes = adminCongeService.getOrphanRequests();

            return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes orphelines récupérées"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============================================================
    // ✅ AJOUT UNIQUE : ENRICHISSEMENT PHOTO POUR /a-valider
    // ============================================================

    private void enrichTasksWithEmployeePhotos(List<Map<String, Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (Map<String, Object> task : tasks) {
            if (task == null) {
                continue;
            }

            Employe employe = findEmployeFromTask(task);

            if (employe == null) {
                task.putIfAbsent("photoUrl", null);
                task.putIfAbsent("employePhotoProfil", null);
                task.putIfAbsent("employePhotoUrl", null);
                continue;
            }

            String photo = employe.getPhotoUrl();

            // ✅ Les 3 noms sont envoyés pour être compatibles avec tous tes composants Angular
            task.put("photoUrl", photo);
            task.put("employePhotoProfil", photo);
            task.put("employePhotoUrl", photo);

            // ✅ Sécurité : si getRHTasks ne les a pas déjà mis
            task.putIfAbsent("employeId", employe.getId());
            task.putIfAbsent("employeNom", employe.getNom());
            task.putIfAbsent("employePrenom", employe.getPrenom());
            task.putIfAbsent("employeEmail", employe.getEmail());
        }
    }

    private Employe findEmployeFromTask(Map<String, Object> task) {
        Long employeId = extractLong(task.get("employeId"));

        if (employeId == null) {
            employeId = extractLong(task.get("employeeId"));
        }

        if (employeId == null) {
            employeId = extractLong(task.get("idEmploye"));
        }

        if (employeId != null) {
            Optional<Employe> byId = employeRepository.findById(employeId);

            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String email = extractString(task.get("employeEmail"));

        if (email == null || email.isBlank()) {
            email = extractString(task.get("employeeEmail"));
        }

        if (email == null || email.isBlank()) {
            email = extractString(task.get("email"));
        }

        if (email != null && !email.isBlank()) {
            Optional<Employe> byEmail = employeRepository.findByEmail(email.trim().toLowerCase());

            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }

        return null;
    }

 
    private String extractString(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String email = jwt.getClaimAsString("email");

        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null) {
            email = jwt.getSubject();
        }

        return email;
    }
}