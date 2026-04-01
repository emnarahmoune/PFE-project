package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;  // ← AJOUT
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

@RestController
@RequestMapping("/api/scores-turnover")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scores de Turnover", description = "API de gestion des scores prédictifs de risque de départ")
public class ScoreTurnoverController {

    private final ScoreTurnoverService scoreTurnoverService;

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

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un score par son ID")
    public ResponseEntity<ApiResponse<ScoreTurnoverDTO>> getScoreById(
            @Parameter(description = "ID du score") @PathVariable Long id) {

        log.info("GET /api/scores-turnover/{}", id);

        return scoreTurnoverService.findById(id)
                .map(score -> ResponseEntity.ok(ApiResponse.success(score, "Score trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère tous les scores d'un employé")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getScoresByEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/scores-turnover/employe/{}", employeId);

        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findByEmployeId(employeId);
        return ResponseEntity.ok(ApiResponse.success(scores, "Scores de l'employé récupérés"));
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

    @GetMapping("/derniers")
    @Operation(summary = "Récupère les derniers scores de tous les employés")
    public ResponseEntity<ApiResponse<List<ScoreTurnoverDTO>>> getDerniersScores() {
        log.info("GET /api/scores-turnover/derniers");
        List<ScoreTurnoverDTO> scores = scoreTurnoverService.findDerniersScores();
        return ResponseEntity.ok(ApiResponse.success(scores, "Derniers scores récupérés"));
    }

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

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un score")
    public ResponseEntity<ApiResponse<Void>> deleteScore(
            @Parameter(description = "ID du score") @PathVariable Long id) {

        log.info("DELETE /api/scores-turnover/{}", id);

        scoreTurnoverService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Score supprimé avec succès"));
    }

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

    @GetMapping("/stats/tableau-bord")
    @Operation(summary = "Récupère les statistiques pour le tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsTableauBord() {
        log.info("GET /api/scores-turnover/stats/tableau-bord");
        Map<String, Object> stats = scoreTurnoverService.getStatsTableauBord();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques tableau de bord récupérées"));
    }
}