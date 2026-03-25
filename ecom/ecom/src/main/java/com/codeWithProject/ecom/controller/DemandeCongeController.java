package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des demandes de congé
 * Endpoints : /api/conges
 */
@RestController
@RequestMapping("/api/conges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Demandes de congé", description = "API de gestion des demandes de congé")
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;

    /**
     * Récupère toutes les demandes de congé
     */
    @GetMapping
    @Operation(summary = "Liste toutes les demandes de congé")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getAllDemandes() {
        log.info("GET /api/conges - Récupération de toutes les demandes");
        List<DemandeCongeDTO> demandes = demandeCongeService.findAll();
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes récupérées avec succès"));
    }

    /**
     * Récupère toutes les demandes avec pagination
     */
    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des demandes de congé")
    public ResponseEntity<ApiResponse<Page<DemandeCongeDTO>>> getAllDemandesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateDemande") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /api/conges/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DemandeCongeDTO> demandes = demandeCongeService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes récupérées avec succès"));
    }

    /**
     * Récupère une demande par son ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère une demande de congé par son ID")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> getDemandeById(
            @Parameter(description = "ID de la demande") @PathVariable Long id) {

        log.info("GET /api/conges/{}", id);

        return demandeCongeService.findById(id)
                .map(demande -> ResponseEntity.ok(ApiResponse.success(demande, "Demande trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère les demandes d'un employé
     */
    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère les demandes d'un employé")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesByEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/conges/employe/{}", employeId);

        List<DemandeCongeDTO> demandes = demandeCongeService.findByEmployeId(employeId);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes de l'employé récupérées"));
    }

    /**
     * Récupère les demandes d'un manager
     */
    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Récupère les demandes à traiter par un manager")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesByManager(
            @Parameter(description = "ID du manager") @PathVariable Long managerId) {

        log.info("GET /api/conges/manager/{}", managerId);

        List<DemandeCongeDTO> demandes = demandeCongeService.findByManagerId(managerId);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes du manager récupérées"));
    }

    /**
     * Récupère les demandes par statut
     */
    @GetMapping("/statut/{statut}")
    @Operation(summary = "Récupère les demandes par statut")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesByStatut(
            @Parameter(description = "Statut (EN_ATTENTE, APPROUVE, REFUSE, ANNULE)") @PathVariable String statut) {

        log.info("GET /api/conges/statut/{}", statut);

        List<DemandeCongeDTO> demandes = demandeCongeService.findByStatut(statut);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes par statut récupérées"));
    }

    /**
     * Récupère les demandes par type
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Récupère les demandes par type de congé")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesByType(
            @Parameter(description = "Type (ANNUEL, MALADIE, SANS_SOLDE, etc.)") @PathVariable String type) {

        log.info("GET /api/conges/type/{}", type);

        List<DemandeCongeDTO> demandes = demandeCongeService.findByType(type);
        return ResponseEntity.ok(ApiResponse.success(demandes, "Demandes par type récupérées"));
    }

    /**
     * Récupère les demandes urgentes
     */
    @GetMapping("/urgentes")
    @Operation(summary = "Récupère les demandes urgentes")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesUrgentes() {
        log.info("GET /api/conges/urgentes");
        List<DemandeCongeDTO> urgentes = demandeCongeService.findDemandesUrgentes();
        return ResponseEntity.ok(ApiResponse.success(urgentes, "Demandes urgentes récupérées"));
    }

    /**
     * Récupère les demandes en attente pour un manager
     */
    @GetMapping("/en-attente/manager/{managerId}")
    @Operation(summary = "Récupère les demandes en attente pour un manager")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getDemandesEnAttentePourManager(
            @Parameter(description = "ID du manager") @PathVariable Long managerId) {

        log.info("GET /api/conges/en-attente/manager/{}", managerId);

        List<DemandeCongeDTO> enAttente = demandeCongeService.findDemandesEnAttentePourManager(managerId);
        return ResponseEntity.ok(ApiResponse.success(enAttente, "Demandes en attente récupérées"));
    }

    /**
     * Récupère les congés en cours
     */
    @GetMapping("/en-cours")
    @Operation(summary = "Récupère les congés en cours")
    public ResponseEntity<ApiResponse<List<DemandeCongeDTO>>> getCongesEnCours() {
        log.info("GET /api/conges/en-cours");
        List<DemandeCongeDTO> enCours = demandeCongeService.findCongesEnCours();
        return ResponseEntity.ok(ApiResponse.success(enCours, "Congés en cours récupérés"));
    }

    /**
     * Crée une nouvelle demande de congé
     */
    @PostMapping
    @Operation(summary = "Crée une nouvelle demande de congé")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> createDemande(
            @Valid @RequestBody DemandeCongeDTO dto) {

        log.info("POST /api/conges - Création d'une demande pour l'employé: {}", dto.getEmployeId());

        DemandeCongeDTO created = demandeCongeService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Demande de congé créée avec succès"));
    }

    /**
     * Modifie une demande de congé (employé)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modifie une demande de congé (employé)")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> modifierDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @Valid @RequestBody DemandeCongeDTO dto) {

        log.info("PUT /api/conges/{} - Modification", id);

        DemandeCongeDTO updated = demandeCongeService.modifier(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Demande modifiée avec succès"));
    }

    /**
     * Annule une demande de congé (employé)
     */
    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annule une demande de congé (employé)")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> annulerDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id) {

        log.info("PUT /api/conges/{}/annuler", id);

        DemandeCongeDTO annulee = demandeCongeService.annuler(id);
        return ResponseEntity.ok(ApiResponse.success(annulee, "Demande annulée avec succès"));
    }

    /**
     * Valide une demande de congé (manager)
     */
    @PutMapping("/{id}/valider/{managerId}")
    @Operation(summary = "Valide une demande de congé (manager)")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> validerDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @Parameter(description = "ID du manager") @PathVariable Long managerId) {

        log.info("PUT /api/conges/{}/valider/{}", id, managerId);

        DemandeCongeDTO validee = demandeCongeService.valider(id, managerId);
        return ResponseEntity.ok(ApiResponse.success(validee, "Demande validée avec succès"));
    }

    /**
     * Refuse une demande de congé (manager)
     */
    @PutMapping("/{id}/refuser/{managerId}")
    @Operation(summary = "Refuse une demande de congé avec motif")
    public ResponseEntity<ApiResponse<DemandeCongeDTO>> refuserDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id,
            @Parameter(description = "ID du manager") @PathVariable Long managerId,
            @RequestParam String motif) {

        log.info("PUT /api/conges/{}/refuser/{} - Motif: {}", id, managerId, motif);

        DemandeCongeDTO refusee = demandeCongeService.refuser(id, managerId, motif);
        return ResponseEntity.ok(ApiResponse.success(refusee, "Demande refusée avec succès"));
    }

    /**
     * Supprime une demande (admin seulement)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une demande de congé (admin)")
    public ResponseEntity<ApiResponse<Void>> deleteDemande(
            @Parameter(description = "ID de la demande") @PathVariable Long id) {

        log.info("DELETE /api/conges/{}", id);

        demandeCongeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Demande supprimée avec succès"));
    }

    /**
     * Statistiques des demandes par statut
     */
    @GetMapping("/stats/statut")
    @Operation(summary = "Statistiques des demandes par statut")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        log.info("GET /api/conges/stats/statut");
        Map<String, Long> stats = demandeCongeService.countByStatut();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par statut récupérées"));
    }

    /**
     * Statistiques des demandes par type
     */
    @GetMapping("/stats/type")
    @Operation(summary = "Statistiques des demandes par type de congé")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByType() {
        log.info("GET /api/conges/stats/type");
        Map<String, Long> stats = demandeCongeService.countByType();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par type récupérées"));
    }

    /**
     * Statistiques mensuelles des demandes
     */
    @GetMapping("/stats/mensuelles/{annee}")
    @Operation(summary = "Statistiques mensuelles des demandes pour une année")
    public ResponseEntity<ApiResponse<Map<Integer, Long>>> getStatsMensuelles(
            @Parameter(description = "Année") @PathVariable int annee) {

        log.info("GET /api/conges/stats/mensuelles/{}", annee);

        Map<Integer, Long> stats = demandeCongeService.getStatsMensuelles(annee);
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques mensuelles récupérées"));
    }

    /**
     * Vérifie les conflits de dates
     */
    @GetMapping("/conflit")
    @Operation(summary = "Vérifie s'il y a conflit de dates pour un employé")
    public ResponseEntity<ApiResponse<Boolean>> checkConflitDates(
            @RequestParam Long employeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long demandeId) {

        log.info("GET /api/conges/conflit - employe: {}, debut: {}, fin: {}", employeId, debut, fin);

        boolean conflit = demandeCongeService.hasConflitDates(employeId, debut, fin, demandeId);
        return ResponseEntity.ok(ApiResponse.success(conflit, "Vérification de conflit effectuée"));
    }
}