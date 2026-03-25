package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.SystemeBIService;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import com.codeWithProject.ecom.service.dto.SystemeBIDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion du système BI
 * Endpoints : /api/systeme-bi
 */
@RestController
@RequestMapping("/api/systeme-bi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Système BI", description = "API de gestion du système de Business Intelligence")
public class SystemeBIController {

    private final SystemeBIService systemeBIService;

    // ===== RECHERCHES GÉNÉRALES =====

    @GetMapping
    @Operation(summary = "Liste tous les systèmes BI")
    public ResponseEntity<ApiResponse<List<SystemeBIDTO>>> getAllSystemes() {
        log.info("GET /api/systeme-bi - Récupération de tous les systèmes BI");
        List<SystemeBIDTO> systemes = systemeBIService.findAll();
        return ResponseEntity.ok(ApiResponse.success(systemes, "Systèmes BI récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des systèmes BI")
    public ResponseEntity<ApiResponse<Page<SystemeBIDTO>>> getAllSystemesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "version") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("GET /api/systeme-bi/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SystemeBIDTO> systemes = systemeBIService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(systemes, "Systèmes BI récupérés avec succès"));
    }

    // ===== RECHERCHES PAR IDENTIFIANTS =====

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un système BI par son ID")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> getSystemeById(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("GET /api/systeme-bi/{}", id);

        return systemeBIService.findById(id)
                .map(systeme -> ResponseEntity.ok(ApiResponse.success(systeme, "Système BI trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/version/{version}")
    @Operation(summary = "Récupère un système BI par sa version")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> getSystemeByVersion(
            @Parameter(description = "Version du système") @PathVariable String version) {

        log.info("GET /api/systeme-bi/version/{}", version);

        return systemeBIService.findByVersion(version)
                .map(systeme -> ResponseEntity.ok(ApiResponse.success(systeme, "Système BI trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== RECHERCHES PAR STATUT =====

    @GetMapping("/statut/actifs")
    @Operation(summary = "Récupère les systèmes BI actifs")
    public ResponseEntity<ApiResponse<List<SystemeBIDTO>>> getSystemesActifs() {
        log.info("GET /api/systeme-bi/statut/actifs");
        List<SystemeBIDTO> actifs = systemeBIService.findSystemesActifs();
        return ResponseEntity.ok(ApiResponse.success(actifs, "Systèmes BI actifs récupérés"));
    }

    @GetMapping("/statut/maintenance")
    @Operation(summary = "Récupère les systèmes BI en maintenance")
    public ResponseEntity<ApiResponse<List<SystemeBIDTO>>> getSystemesEnMaintenance() {
        log.info("GET /api/systeme-bi/statut/maintenance");
        List<SystemeBIDTO> maintenance = systemeBIService.findSystemesEnMaintenance();
        return ResponseEntity.ok(ApiResponse.success(maintenance, "Systèmes BI en maintenance récupérés"));
    }

    // ===== CRUD =====

    @PostMapping
    @Operation(summary = "Crée un nouveau système BI")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> createSysteme(
            @Valid @RequestBody SystemeBIDTO dto) {

        log.info("POST /api/systeme-bi - Création d'un système BI version: {}", dto.getVersion());

        SystemeBIDTO created = systemeBIService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Système BI créé avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un système BI")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> updateSysteme(
            @Parameter(description = "ID du système BI") @PathVariable Long id,
            @Valid @RequestBody SystemeBIDTO dto) {

        log.info("PUT /api/systeme-bi/{} - Mise à jour", id);

        SystemeBIDTO updated = systemeBIService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Système BI mis à jour avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un système BI")
    public ResponseEntity<ApiResponse<Void>> deleteSysteme(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("DELETE /api/systeme-bi/{}", id);

        systemeBIService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Système BI supprimé avec succès"));
    }

    // ===== GESTION DU STATUT =====

    @PatchMapping("/{id}/activer")
    @Operation(summary = "Active un système BI")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> activerSysteme(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("PATCH /api/systeme-bi/{}/activer", id);

        SystemeBIDTO active = systemeBIService.activer(id);
        return ResponseEntity.ok(ApiResponse.success(active, "Système BI activé avec succès"));
    }

    @PatchMapping("/{id}/maintenance")
    @Operation(summary = "Met un système BI en maintenance")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> mettreEnMaintenance(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("PATCH /api/systeme-bi/{}/maintenance", id);

        SystemeBIDTO maintenance = systemeBIService.mettreEnMaintenance(id);
        return ResponseEntity.ok(ApiResponse.success(maintenance, "Système BI mis en maintenance"));
    }

    @PatchMapping("/{id}/desactiver")
    @Operation(summary = "Désactive un système BI")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> desactiverSysteme(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("PATCH /api/systeme-bi/{}/desactiver", id);

        SystemeBIDTO desactive = systemeBIService.desactiver(id);
        return ResponseEntity.ok(ApiResponse.success(desactive, "Système BI désactivé avec succès"));
    }

    // ===== OPÉRATIONS BI =====

    @PostMapping("/{id}/etl/executer")
    @Operation(summary = "Exécute l'ETL pour un système BI")
    public ResponseEntity<ApiResponse<SystemeBIDTO>> executerETL(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("POST /api/systeme-bi/{}/etl/executer", id);

        SystemeBIDTO result = systemeBIService.executerETL(id);
        return ResponseEntity.ok(ApiResponse.success(result, "ETL exécuté avec succès"));
    }

    @GetMapping("/{id}/analyse/competences")
    @Operation(summary = "Analyse les compétences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyserCompetences(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("GET /api/systeme-bi/{}/analyse/competences", id);

        Map<String, Object> result = systemeBIService.analyserCompetences(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Analyse des compétences réalisée"));
    }

    @GetMapping("/{id}/recommandations/formations/{employeId}")
    @Operation(summary = "Recommande des formations pour un employé")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> recommanderFormations(
            @Parameter(description = "ID du système BI") @PathVariable Long id,
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/systeme-bi/{}/recommandations/formations/{}", id, employeId);

        List<FormationDTO> recommandations = systemeBIService.recommanderFormations(id, employeId);
        return ResponseEntity.ok(ApiResponse.success(recommandations, "Recommandations de formations récupérées"));
    }

    @PostMapping("/{id}/analyse/turnover")
    @Operation(summary = "Analyse le turnover")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> analyserTurnover(
            @Parameter(description = "ID du système BI") @PathVariable Long id,
            @RequestParam String periode,
            @RequestParam(required = false) String departement) {

        log.info("POST /api/systeme-bi/{}/analyse/turnover - période: {}, département: {}", id, periode, departement);

        IndicateurRHDTO indicateur = systemeBIService.analyserTurnover(id, periode, departement);
        return ResponseEntity.ok(ApiResponse.success(indicateur, "Analyse du turnover réalisée"));
    }

    @PostMapping("/{id}/analyse/absenteisme")
    @Operation(summary = "Analyse l'absentéisme")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> analyserAbsenteisme(
            @Parameter(description = "ID du système BI") @PathVariable Long id,
            @RequestParam String periode,
            @RequestParam(required = false) String departement) {

        log.info("POST /api/systeme-bi/{}/analyse/absenteisme - période: {}, département: {}", id, periode, departement);

        IndicateurRHDTO indicateur = systemeBIService.analyserAbsenteisme(id, periode, departement);
        return ResponseEntity.ok(ApiResponse.success(indicateur, "Analyse de l'absentéisme réalisée"));
    }

    @PostMapping("/{id}/predire/turnover/tous")
    @Operation(summary = "Prédit le turnover pour tous les employés")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> predireTurnoverTous(
            @Parameter(description = "ID du système BI") @PathVariable Long id) {

        log.info("POST /api/systeme-bi/{}/predire/turnover/tous", id);

        List<ScoreTurnoverDTO> scores = systemeBIService.predireTurnover(id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(scores, "Prédictions de turnover réalisées"));
    }

    @PostMapping("/{id}/predire/turnover/employe/{employeId}")
    @Operation(summary = "Prédit le turnover pour un employé spécifique")
    public ResponseEntity<ApiResponse<ScoreTurnoverDTO>> predireTurnoverEmploye(
            @Parameter(description = "ID du système BI") @PathVariable Long id,
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("POST /api/systeme-bi/{}/predire/turnover/employe/{}", id, employeId);

        ScoreTurnoverDTO score = systemeBIService.predireTurnoverEmploye(id, employeId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(score, "Prédiction de turnover réalisée"));
    }

    // ===== STATISTIQUES =====

    @GetMapping("/stats/statut")
    @Operation(summary = "Compte les systèmes BI par statut")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countByStatut() {
        log.info("GET /api/systeme-bi/stats/statut");
        Map<String, Long> stats = systemeBIService.countByStatut();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par statut récupérées"));
    }

    @GetMapping("/stats/derniere-execution-globale")
    @Operation(summary = "Récupère la dernière exécution globale")
    public ResponseEntity<ApiResponse<LocalDateTime>> getDerniereExecutionGlobale() {
        log.info("GET /api/systeme-bi/stats/derniere-execution-globale");
        LocalDateTime date = systemeBIService.getDerniereExecutionGlobale();
        return ResponseEntity.ok(ApiResponse.success(date, "Dernière exécution globale récupérée"));
    }

    @GetMapping("/stats/tableau-bord")
    @Operation(summary = "Récupère les statistiques pour le tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsTableauBord() {
        log.info("GET /api/systeme-bi/stats/tableau-bord");
        Map<String, Object> stats = systemeBIService.getStatsTableauBord();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques tableau de bord récupérées"));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des systèmes BI par mot-clé")
    public ResponseEntity<ApiResponse<List<SystemeBIDTO>>> searchSystemes(
            @RequestParam String keyword) {

        log.info("GET /api/systeme-bi/search?keyword={}", keyword);

        List<SystemeBIDTO> result = systemeBIService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }
}