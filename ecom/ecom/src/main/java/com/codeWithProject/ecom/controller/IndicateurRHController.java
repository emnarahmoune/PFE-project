package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;  // ← AJOUT
import com.codeWithProject.ecom.service.IndicateurRHService;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
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

@RestController
@RequestMapping("/api/indicateurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Indicateurs RH", description = "API de gestion des indicateurs RH")
public class IndicateurRHController {

    private final IndicateurRHService indicateurRHService;

    @GetMapping
    @Operation(summary = "Liste tous les indicateurs")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getAllIndicateurs() {
        log.info("GET /api/indicateurs - Récupération de tous les indicateurs");
        List<IndicateurRHDTO> indicateurs = indicateurRHService.findAll();
        return ResponseEntity.ok(ApiResponse.success(indicateurs, "Indicateurs récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des indicateurs")
    public ResponseEntity<ApiResponse<Page<IndicateurRHDTO>>> getAllIndicateursPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCalcul") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /api/indicateurs/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<IndicateurRHDTO> indicateurs = indicateurRHService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(indicateurs, "Indicateurs récupérés avec succès"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un indicateur par son ID")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> getIndicateurById(
            @Parameter(description = "ID de l'indicateur") @PathVariable Long id) {

        log.info("GET /api/indicateurs/{}", id);

        return indicateurRHService.findById(id)
                .map(indicateur -> ResponseEntity.ok(ApiResponse.success(indicateur, "Indicateur trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Récupère les indicateurs par type")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getIndicateursByType(
            @Parameter(description = "Type (TURNOVER, ABSENTEISME, PERFORMANCE, etc.)") @PathVariable String type) {

        log.info("GET /api/indicateurs/type/{}", type);

        List<IndicateurRHDTO> indicateurs = indicateurRHService.findByType(type);
        return ResponseEntity.ok(ApiResponse.success(indicateurs, "Indicateurs par type récupérés"));
    }

    @GetMapping("/periode/{periode}")
    @Operation(summary = "Récupère les indicateurs par période")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getIndicateursByPeriode(
            @Parameter(description = "Période (MENSUEL, TRIMESTRIEL, ANNUEL)") @PathVariable String periode) {

        log.info("GET /api/indicateurs/periode/{}", periode);

        List<IndicateurRHDTO> indicateurs = indicateurRHService.findByPeriode(periode);
        return ResponseEntity.ok(ApiResponse.success(indicateurs, "Indicateurs par période récupérés"));
    }

    @GetMapping("/departement/{departement}")
    @Operation(summary = "Récupère les indicateurs par département")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getIndicateursByDepartement(
            @Parameter(description = "Nom du département") @PathVariable String departement) {

        log.info("GET /api/indicateurs/departement/{}", departement);

        List<IndicateurRHDTO> indicateurs = indicateurRHService.findByDepartement(departement);
        return ResponseEntity.ok(ApiResponse.success(indicateurs, "Indicateurs par département récupérés"));
    }

    @GetMapping("/dernier/{type}")
    @Operation(summary = "Récupère le dernier indicateur d'un type")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> getDernierIndicateurByType(
            @Parameter(description = "Type d'indicateur") @PathVariable String type) {

        log.info("GET /api/indicateurs/dernier/{}", type);

        return indicateurRHService.findDernierIndicateurByType(type)
                .map(indicateur -> ResponseEntity.ok(ApiResponse.success(indicateur, "Dernier indicateur trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recents/{jours}")
    @Operation(summary = "Récupère les indicateurs des X derniers jours")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getIndicateursRecents(
            @Parameter(description = "Nombre de jours") @PathVariable int jours) {

        log.info("GET /api/indicateurs/recents/{}", jours);

        List<IndicateurRHDTO> recents = indicateurRHService.findIndicateursRecents(jours);
        return ResponseEntity.ok(ApiResponse.success(recents, "Indicateurs récents récupérés"));
    }

    @GetMapping("/alertes")
    @Operation(summary = "Récupère les indicateurs avec alerte")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getIndicateursAvecAlerte() {
        log.info("GET /api/indicateurs/alertes");
        List<IndicateurRHDTO> alertes = indicateurRHService.findAvecAlerte();
        return ResponseEntity.ok(ApiResponse.success(alertes, "Indicateurs avec alerte récupérés"));
    }

    @PostMapping
    @Operation(summary = "Crée un nouvel indicateur")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> createIndicateur(
            @Valid @RequestBody IndicateurRHDTO dto) {

        log.info("POST /api/indicateurs - Création d'un indicateur de type: {}", dto.getType());

        IndicateurRHDTO created = indicateurRHService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Indicateur créé avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un indicateur")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> updateIndicateur(
            @Parameter(description = "ID de l'indicateur") @PathVariable Long id,
            @Valid @RequestBody IndicateurRHDTO dto) {

        log.info("PUT /api/indicateurs/{} - Mise à jour", id);

        IndicateurRHDTO updated = indicateurRHService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Indicateur mis à jour avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un indicateur")
    public ResponseEntity<ApiResponse<Void>> deleteIndicateur(
            @Parameter(description = "ID de l'indicateur") @PathVariable Long id) {

        log.info("DELETE /api/indicateurs/{}", id);

        indicateurRHService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Indicateur supprimé avec succès"));
    }

    @PostMapping("/calculer/turnover")
    @Operation(summary = "Calcule un indicateur de turnover")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> calculerTurnover(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "ANNUEL") String periode,
            @RequestParam(required = false) String departement) {

        log.info("POST /api/indicateurs/calculer/turnover - du {} au {}", dateDebut, dateFin);

        IndicateurRHDTO indicateur = indicateurRHService.calculerTurnover(dateDebut, dateFin, periode, departement);
        return ResponseEntity.ok(ApiResponse.success(indicateur, "Turnover calculé avec succès"));
    }

    @PostMapping("/calculer/absenteisme")
    @Operation(summary = "Calcule un indicateur d'absentéisme")
    public ResponseEntity<ApiResponse<IndicateurRHDTO>> calculerAbsenteisme(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "MENSUEL") String periode,
            @RequestParam(required = false) String departement) {

        log.info("POST /api/indicateurs/calculer/absenteisme - du {} au {}", dateDebut, dateFin);

        IndicateurRHDTO indicateur = indicateurRHService.calculerAbsenteisme(dateDebut, dateFin, periode, departement);
        return ResponseEntity.ok(ApiResponse.success(indicateur, "Absentéisme calculé avec succès"));
    }

    @GetMapping("/stats/moyennes")
    @Operation(summary = "Récupère les moyennes des indicateurs par type")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getMoyennesByType() {
        log.info("GET /api/indicateurs/stats/moyennes");
        Map<String, Double> moyennes = indicateurRHService.getMoyennesByType();
        return ResponseEntity.ok(ApiResponse.success(moyennes, "Moyennes par type récupérées"));
    }

    @GetMapping("/stats/historique/{type}")
    @Operation(summary = "Récupère l'historique d'un indicateur")
    public ResponseEntity<ApiResponse<List<IndicateurRHDTO>>> getHistoriqueIndicateur(
            @Parameter(description = "Type d'indicateur") @PathVariable String type,
            @RequestParam(defaultValue = "12") int limite) {

        log.info("GET /api/indicateurs/stats/historique/{}?limite={}", type, limite);

        List<IndicateurRHDTO> historique = indicateurRHService.getHistoriqueIndicateur(type, limite);
        return ResponseEntity.ok(ApiResponse.success(historique, "Historique récupéré"));
    }

    @GetMapping("/stats/tableau-bord")
    @Operation(summary = "Récupère les statistiques pour le tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsTableauBord() {
        log.info("GET /api/indicateurs/stats/tableau-bord");
        Map<String, Object> stats = indicateurRHService.getStatsTableauBord();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques tableau de bord récupérées"));
    }
}