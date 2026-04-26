package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.AdminCongeService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
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
@RequestMapping("/api/admin/conges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin RH - Gestion des congés", description = "API pour l'administration des demandes de congé")
@PreAuthorize("hasRole('ADMIN_RH') or hasRole('admin')")
public class AdminCongeController {

    private final AdminCongeService adminCongeService;
    private final WorkflowService workflowService;

    @GetMapping("/a-valider")
    @Operation(summary = "Récupère les demandes en attente (tâches Camunda)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDemandesAValider(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/admin/conges/a-valider - Récupération des tâches RH");
        String adminEmail = extractEmail(jwt);
        try {
            List<Map<String, Object>> tasks = workflowService.getRHTasks(adminEmail);
            return ResponseEntity.ok(ApiResponse.success(tasks, "Tâches récupérées avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ NOUVEAU : récupérer les demandes refusées par les managers
    @GetMapping("/refus-manager")
    @Operation(summary = "Récupère les demandes refusées par les managers")
    public ResponseEntity<ApiResponse<List<DemandeCongeAdminDTO>>> getDemandesRefuseesParManager() {
        log.info("GET /api/admin/conges/refus-manager");
        try {
            List<DemandeCongeAdminDTO> demandes = adminCongeService.getDemandesRefuseesParManager();
            return ResponseEntity.ok(ApiResponse.success(demandes, "Refus manager récupérés"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
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
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
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
            log.error("Erreur: {}", e.getMessage());
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
            log.error("Erreur lors de l'approbation: {}", e.getMessage());
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
            log.error("Erreur lors du refus: {}", e.getMessage());
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
            log.error("Erreur: {}", e.getMessage());
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
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
}