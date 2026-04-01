package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
import com.codeWithProject.ecom.service.dto.SoldeCongesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Demandes de congé", description = "API de gestion des demandes de congé")
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;

    // ==================== ENDPOINTS EMPLOYÉ ====================

    @PostMapping
    @Operation(summary = "Crée une nouvelle demande de congé")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> createDemande(
            @Valid @RequestBody DemandeCongeDTO dto,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /api/conges - Création d'une demande de congé");

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        log.info("Email récupéré depuis le token: {}", email);

        try {
            DemandeCongeDTO created = demandeCongeService.createForAuthenticatedUser(dto, email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(created, "Demande de congé créée avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la création: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @GetMapping("/mes-conges")
    @Operation(summary = "Récupère mes demandes de congé")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getMesDemandes(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("GET /api/conges/mes-conges");

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        try {
            List<DemandeCongeDTO> demandes = demandeCongeService.findByEmployeEmail(email);
            return ResponseEntity.ok(ApiResponse.success(demandes, "Mes demandes récupérées"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @GetMapping("/mon-solde-conges")
    @Operation(summary = "Récupère mon solde de congés")
    public ResponseEntity<ApiResponse<SoldeCongesDTO>> getMonSoldeConges(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("GET /api/conges/mon-solde-conges");

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        try {
            SoldeCongesDTO solde = demandeCongeService.getSoldeCongesByEmail(email);
            return ResponseEntity.ok(ApiResponse.success(solde, "Solde de congés récupéré"));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annule une demande de congé")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> annulerDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /api/conges/{}/annuler", id);

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        try {
            DemandeCongeDTO annulee = demandeCongeService.annulerForAuthenticatedUser(id, email);
            return ResponseEntity.ok(ApiResponse.success(annulee, "Demande annulée avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de l'annulation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    // Endpoint alternatif pour la compatibilité avec le frontend qui utilise /api/conges/annuler/{id}
    @PutMapping("/annuler/{id}")
    @Operation(summary = "Annule une demande de congé (endpoint alternatif)")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> annulerDemandeAlternatif(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /api/conges/annuler/{} - Endpoint alternatif", id);
        return annulerDemande(id, jwt);
    }

    // ==================== ENDPOINTS ADMIN ====================

    @GetMapping("/admin/all")
    @Operation(summary = "Récupère toutes les demandes (admin)")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getAllDemandesForAdmin() {
        log.info("GET /api/conges/admin/all - Admin");
        List<DemandeCongeDTO> demandes = demandeCongeService.findAll();
        return ResponseEntity.ok(ApiResponse.success(demandes, "Toutes les demandes récupérées"));
    }

    @GetMapping("/admin/employe/{employeId}")
    @Operation(summary = "Récupère les demandes d'un employé (admin)")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesByEmployeForAdmin(
            @PathVariable Long employeId) {
        log.info("GET /api/conges/admin/employe/{}", employeId);
        List<DemandeCongeDTO> demandes = demandeCongeService.findByEmployeId(employeId);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes de l'employé récupérées"));
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Statistiques des demandes (admin)")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsForAdmin() {
        log.info("GET /api/conges/admin/stats");
        Map<String, Long> stats = demandeCongeService.countByStatut();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées"));
    }

    // ==================== ENDPOINTS GÉNÉRAUX ====================

    @GetMapping("/{id}")
    @Operation(summary = "Récupère une demande par son ID")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> getDemandeById(@PathVariable Long id) {
        log.info("GET /api/conges/{}", id);
        return demandeCongeService.findById(id)
                .map(demande -> ResponseEntity.ok(ApiResponse.success(demande, "Demande trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Récupère les demandes par statut")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesByStatut(@PathVariable String statut) {
        log.info("GET /api/conges/statut/{}", statut);
        List<DemandeCongeDTO> demandes = demandeCongeService.findByStatut(statut);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes par statut récupérées"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une demande (admin)")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<ApiResponse<Void>> deleteDemande(@PathVariable Long id) {
        log.info("DELETE /api/conges/{}", id);
        demandeCongeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Demande supprimée avec succès"));
    }

    @GetMapping("/conflit")
    @Operation(summary = "Vérifie les conflits de dates")
    public ResponseEntity<ApiResponse<Boolean>> checkConflitDates(
            @RequestParam Long employeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long demandeId) {

        log.info("GET /api/conges/conflit - employe: {}, debut: {}, fin: {}", employeId, debut, fin);
        boolean conflit = demandeCongeService.hasConflitDates(employeId, debut, fin, demandeId);
        return ResponseEntity.ok(ApiResponse.success(conflit, "Vérification de conflit effectuée"));
    }

    // ==================== ENDPOINTS URGENTS ====================

    @GetMapping("/urgentes")
    @Operation(summary = "Récupère les demandes urgentes en attente")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesUrgentes() {
        log.info("GET /api/conges/urgentes");
        List<DemandeCongeDTO> urgentes = demandeCongeService.findUrgentesEnAttente();
        return ResponseEntity.ok(ApiResponse.success(urgentes, "Demandes urgentes récupérées"));
    }

    // ==================== ENDPOINTS STATISTIQUES ====================

    @GetMapping("/stats/statut")
    @Operation(summary = "Statistiques des demandes par statut")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        log.info("GET /api/conges/stats/statut");
        Map<String, Long> stats = demandeCongeService.countByStatut();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par statut récupérées"));
    }
}