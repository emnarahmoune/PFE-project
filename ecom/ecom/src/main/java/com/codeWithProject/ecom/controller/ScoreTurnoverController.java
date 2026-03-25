package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.ScoreTurnoverService;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des scores de turnover
 * Endpoints : /api/scores-turnover
 */
@RestController
@RequestMapping("/api/scores-turnover")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scores de Turnover", description = "API de gestion des scores prédictifs de risque de départ")
public class ScoreTurnoverController {

    private final ScoreTurnoverService scoreTurnoverService;

    // ===== RECHERCHES GÉNÉRALES =====

    @GetMapping
    @Operation(summary = "Liste tous les scores de turnover")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getAllScores() {
        log.info("GET /api/scores-turnover - Récupération de tous les scores");
        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findAll();
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des scores")
    public ResponseEntity<ApiResponse<Page<ScoreTurnoverDTO>>> getAllScoresPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "datePrediction") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /api/scores-turnover/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ScoreTurnoverDTO> scores = scoreTurnoverService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores récupérés avec succès"));
    }

    // ===== RECHERCHES PAR IDENTIFIANTS =====

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un score par son ID")
    public ResponseEntity<ApiResponse<ScoreTurnoverDTO>> getScoreById(
            @Parameter(description = "ID du score") @PathVariable Long id) {

        log.info("GET /api/scores-turnover/{}", id);

        return scoreTurnoverService.findById(id)
                .map(score -> ResponseEntity.ok(ApiResponse.success(score, "Score trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== RECHERCHES PAR EMPLOYÉ =====

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère tous les scores d'un employé")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getScoresByEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/scores-turnover/employe/{}", employeId);

        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findByEmployeId(employeId);
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores de l'employé récupérés"));
    }

    @GetMapping("/employe/{employeId}/historique")
    @Operation(summary = "Récupère l'historique des scores d'un employé")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getHistoriqueEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/scores-turnover/employe/{}/historique", employeId);

        List<ScoreTurnoverDTO> historique = scoreTurnoverService.findHistoriqueEmploye(employeId);
        return ResponseEntity.ok(ApiResponse.success(historique, "Historique des scores récupéré"));
    }

    @GetMapping("/employe/{employeId}/dernier")
    @Operation(summary = "Récupère le dernier score d'un employé")
    public ResponseEntity<ApiResponse<ScoreTurnoverDTO>> getDernierScoreEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/scores-turnover/employe/{}/dernier", employeId);

        return scoreTurnoverService.findDernierScoreEmploye(employeId)
                .map(score -> ResponseEntity.ok(ApiResponse.success(score, "Dernier score trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== RECHERCHES PAR NIVEAU DE RISQUE =====

    @GetMapping("/risque/{niveau}")
    @Operation(summary = "Récupère les scores par niveau de risque")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getScoresByNiveauRisque(
            @Parameter(description = "Niveau de risque (FAIBLE, MOYEN, ELEVE, CRITIQUE)") @PathVariable String niveau) {

        log.info("GET /api/scores-turnover/risque/{}", niveau);

        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findByNiveauRisque(niveau);
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores par niveau de risque récupérés"));
    }

    @GetMapping("/risques-eleves")
    @Operation(summary = "Récupère les scores à risque élevé ou critique")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getScoresRisques() {
        log.info("GET /api/scores-turnover/risques-eleves");
        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findScoresRisques();
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores à risque récupérés"));
    }

    @GetMapping("/critiques")
    @Operation(summary = "Récupère les scores critiques")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getScoresCritiques() {
        log.info("GET /api/scores-turnover/critiques");
        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findScoresCritiques();
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores critiques récupérés"));
    }

    // ===== DERNIERS SCORES =====

    @GetMapping("/derniers")
    @Operation(summary = "Récupère les derniers scores de tous les employés")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getDerniersScores() {
        log.info("GET /api/scores-turnover/derniers");
        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findDerniersScores();
        return ResponseEntity.ok(ApiResponse.success(scores, "Derniers scores récupérés"));
    }

    // ===== CALCULS ET PRÉDICTIONS =====

    @PostMapping("/calculer/employe/{employeId}")
    @Operation(summary = "Calcule un nouveau score pour un employé")
    public ResponseEntity<ApiResponse<ScoreTurnoverDTO>> calculerScorePourEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId,
            @RequestParam Long systemeBIId) {

        log.info("POST /api/scores-turnover/calculer/employe/{} - système BI: {}", employeId, systemeBIId);

        ScoreTurnoverDTO score = scoreTurnoverService.calculerScorePourEmploye(employeId, systemeBIId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(score, "Score calculé avec succès"));
    }

    @PostMapping("/calculer/tous")
    @Operation(summary = "Calcule les scores pour tous les employés")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> calculerScoresPourTousEmployes(
            @RequestParam Long systemeBIId) {

        log.info("POST /api/scores-turnover/calculer/tous - système BI: {}", systemeBIId);

        List<ScoreTurnoverDTO> scores = scoreTurnoverService.calculerScoresPourTousEmployes(systemeBIId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(scores, "Scores calculés pour tous les employés"));
    }

    @PostMapping("/predire/employe/{employeId}")
    @Operation(summary = "Prédit le risque de départ pour un employé")
    public ResponseEntity<ApiResponse<ScoreTurnoverDTO>> predireRisque(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId,
            @RequestParam Long systemeBIId) {

        log.info("POST /api/scores-turnover/predire/employe/{} - système BI: {}", employeId, systemeBIId);

        ScoreTurnoverDTO score = scoreTurnoverService.predireRisque(employeId, systemeBIId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(score, "Risque prédit avec succès"));
    }

    // ===== GESTION DES SCORES =====

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un score")
    public ResponseEntity<ApiResponse<Void>> deleteScore(
            @Parameter(description = "ID du score") @PathVariable Long id) {

        log.info("DELETE /api/scores-turnover/{}", id);

        scoreTurnoverService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Score supprimé avec succès"));
    }

    @DeleteMapping("/obsoletes/{jours}")
    @Operation(summary = "Supprime les scores obsolètes (plus de X jours)")
    public ResponseEntity<ApiResponse<Void>> deleteScoresObsoletes(
            @Parameter(description = "Nombre de jours") @PathVariable int jours) {

        log.info("DELETE /api/scores-turnover/obsoletes/{}", jours);

        scoreTurnoverService.deleteScoresObsoletes(jours);
        return ResponseEntity.ok(ApiResponse.success("Scores obsolètes supprimés avec succès"));
    }

    // ===== STATISTIQUES =====

    @GetMapping("/stats/repartition-risques")
    @Operation(summary = "Récupère la répartition des risques actuels")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRepartitionRisques() {
        log.info("GET /api/scores-turnover/stats/repartition-risques");
        Map<String, Long> repartition = scoreTurnoverService.getRepartitionRisques();
        return ResponseEntity.ok(ApiResponse.success(repartition, "Répartition des risques récupérée"));
    }

    @GetMapping("/stats/score-moyen-actuel")
    @Operation(summary = "Calcule le score moyen actuel")
    public ResponseEntity<ApiResponse<Double>> getScoreMoyenActuel() {
        log.info("GET /api/scores-turnover/stats/score-moyen-actuel");
        Double moyenne = scoreTurnoverService.getScoreMoyenActuel();
        return ResponseEntity.ok(ApiResponse.success(moyenne, "Score moyen actuel calculé"));
    }

    @GetMapping("/stats/score-moyen-par-departement")
    @Operation(summary = "Calcule le score moyen par département")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getScoreMoyenParDepartement() {
        log.info("GET /api/scores-turnover/stats/score-moyen-par-departement");
        Map<String, Double> stats = scoreTurnoverService.getScoreMoyenParDepartement();
        return ResponseEntity.ok(ApiResponse.success(stats, "Score moyen par département calculé"));
    }

    @GetMapping("/stats/avec-actions")
    @Operation(summary = "Récupère les scores avec actions recommandées")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getScoresAvecActions() {
        log.info("GET /api/scores-turnover/stats/avec-actions");
        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findScoresAvecActions();
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores avec actions recommandées récupérés"));
    }

    @GetMapping("/stats/tableau-bord")
    @Operation(summary = "Récupère les statistiques pour le tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsTableauBord() {
        log.info("GET /api/scores-turnover/stats/tableau-bord");
        Map<String, Object> stats = scoreTurnoverService.getStatsTableauBord();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques tableau de bord récupérées"));
    }

    @GetMapping("/employe/{employeId}/score-recent")
    @Operation(summary = "Vérifie si un employé a un score récent")
    public ResponseEntity<ApiResponse<Boolean>> hasScoreRecent(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId,
            @RequestParam(defaultValue = "30") int jours) {

        log.info("GET /api/scores-turnover/employe/{}/score-recent?jours={}", employeId, jours);

        boolean recent = scoreTurnoverService.hasScoreRecent(employeId, jours);
        return ResponseEntity.ok(ApiResponse.success(recent, "Vérification de score récent effectuée"));
    }
}