package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Notification;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.NotificationService;
import com.codeWithProject.ecom.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final NotificationService notificationService;
    private final EmployeRepository employeRepository;
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


    @GetMapping("/calendar-events")
    @Operation(summary = "Récupère les événements pour le calendrier FullCalendar")
    public ResponseEntity<List<CalendarEventDTO>> getCalendarEvents() {
        log.info("GET /api/conges/calendar-events");
        return ResponseEntity.ok(demandeCongeService.getAllCalendarEvents());
    }

    @GetMapping("/demandes/{id}/refus-details")
    @Operation(summary = "Détails complets d'une demande refusée (avec heure de soumission et de refus manager)")
    public ResponseEntity<DemandeRefusDetailsDTO> getRefusDetails(@PathVariable Long id) {
        log.info("GET /api/demandes/{}/refus-details", id);
        return ResponseEntity.ok(demandeCongeService.getRefusDetails(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie une demande de congé existante (seulement si en attente)")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> modifierDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @Valid @RequestBody DemandeCongeDTO dto,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /api/conges/{} - Modification d'une demande", id);

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        try {
            DemandeCongeDTO updated = demandeCongeService.modifierForAuthenticatedUser(id, dto, email);
            return ResponseEntity.ok(ApiResponse.success(updated, "Demande modifiée avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la modification: {}", e.getMessage(), e);
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

    @PutMapping("/annuler/{id}")
    @Operation(summary = "Annule une demande de congé (endpoint alternatif)")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> annulerDemandeAlternatif(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /api/conges/annuler/{} - Endpoint alternatif", id);
        return annulerDemande(id, jwt);
    }

    // ==================== ENDPOINTS NOTIFICATIONS ====================

    @GetMapping("/notifications")
    @Operation(summary = "Récupère les notifications de l'utilisateur connecté")
    public ResponseEntity<ApiResponse<List<Notification>>> getMesNotifications(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("GET /api/conges/notifications - Récupération des notifications");

        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();

        try {
            var employeOpt = employeRepository.findByEmail(email);
            if (employeOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(List.of(), "Aucune notification (employé non trouvé)"));
            }
            Long employeId = employeOpt.get().getId();
            List<Notification> notifications = notificationService.getNotificationsByEmployeId(employeId);
            return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications récupérées"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur serveur", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @PutMapping("/notifications/{id}/read")
    @Operation(summary = "Marque une notification comme lue")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(@PathVariable Long id) {
        log.info("PUT /api/conges/notifications/{}/read", id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marquée comme lue"));
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

    @GetMapping("/urgentes")
    @Operation(summary = "Récupère les demandes urgentes en attente")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'ADMIN', 'manager')")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesUrgentes() {
        log.info("GET /api/conges/urgentes");
        List<DemandeCongeDTO> urgentes = demandeCongeService.findUrgentesEnAttente();
        return ResponseEntity.ok(ApiResponse.success(urgentes, "Demandes urgentes récupérées"));
    }

    @GetMapping("/stats/statut")
    @Operation(summary = "Statistiques des demandes par statut")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'ADMIN', 'manager')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        log.info("GET /api/conges/stats/statut");
        Map<String, Long> stats = demandeCongeService.countByStatut();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par statut récupérées"));
    }
    @GetMapping("/refus-manager")
    @Operation(summary = "Liste des demandes refusées par un manager (pour admin)")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<List<DemandeRefusManagerDTO>> getDemandesRefuseesParManager() {
        log.info("GET /api/conges/refus-manager");
        return ResponseEntity.ok(demandeCongeService.getDemandesRefuseesParManager());
    }
    // ===== NOUVEAU : Récupérer les congés d'un employé (pour manager ou admin RH) =====
    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère les congés d'un employé (réservé au manager de cet employé ou admin RH)")
    @PreAuthorize("hasRole('manager') or hasRole('ADMIN_RH')")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getCongesByEmployeForManager(
            @PathVariable Long employeId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/conges/employe/{}", employeId);
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        List<DemandeCongeDTO> conges = demandeCongeService.getCongesByEmployeIdForManager(employeId, email);
        return ResponseEntity.ok(ApiResponse.success(conges, "Historique des congés récupéré"));
    }
}