package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.FormationService;
import com.codeWithProject.ecom.service.dto.FormationDTO;
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

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des formations
 * Endpoints : /api/formations
 */
@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Formations", description = "API de gestion des formations")
public class FormationController {

    private final FormationService formationService;

    // ===== RECHERCHES GÉNÉRALES =====

    @GetMapping
    @Operation(summary = "Liste toutes les formations")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getAllFormations() {
        log.info("GET /api/formations - Récupération de toutes les formations");
        List<FormationDTO> formations = formationService.findAll();
        return ResponseEntity.ok(ApiResponse.success(formations, "Formations récupérées avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des formations")
    public ResponseEntity<ApiResponse<Page<FormationDTO>>> getAllFormationsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titre") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("GET /api/formations/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FormationDTO> formations = formationService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(formations, "Formations récupérées avec succès"));
    }

    // ===== RECHERCHES PAR IDENTIFIANTS =====

    @GetMapping("/{id}")
    @Operation(summary = "Récupère une formation par son ID")
    public ResponseEntity<ApiResponse<FormationDTO>> getFormationById(
            @Parameter(description = "ID de la formation") @PathVariable Long id) {

        log.info("GET /api/formations/{}", id);

        return formationService.findById(id)
                .map(formation -> ResponseEntity.ok(ApiResponse.success(formation, "Formation trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/titre/{titre}")
    @Operation(summary = "Récupère une formation par son titre")
    public ResponseEntity<ApiResponse<FormationDTO>> getFormationByTitre(
            @Parameter(description = "Titre de la formation") @PathVariable String titre) {

        log.info("GET /api/formations/titre/{}", titre);

        return formationService.findByTitre(titre)
                .map(formation -> ResponseEntity.ok(ApiResponse.success(formation, "Formation trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== RECHERCHES PAR ATTRIBUTS =====

    @GetMapping("/domaine/{domaine}")
    @Operation(summary = "Récupère les formations par domaine")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getFormationsByDomaine(
            @Parameter(description = "Domaine de formation") @PathVariable String domaine) {

        log.info("GET /api/formations/domaine/{}", domaine);

        List<FormationDTO> formations = formationService.findByDomaine(domaine);
        return ResponseEntity.ok(ApiResponse.success(formations, "Formations par domaine récupérées"));
    }

    @GetMapping("/actives")
    @Operation(summary = "Récupère les formations actives")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getFormationsActives() {
        log.info("GET /api/formations/actives");
        List<FormationDTO> actives = formationService.findActives();
        return ResponseEntity.ok(ApiResponse.success(actives, "Formations actives récupérées"));
    }

    @GetMapping("/domaines")
    @Operation(summary = "Récupère tous les domaines de formation")
    public ResponseEntity<ApiResponse<List<String>>> getAllDomaines() {
        log.info("GET /api/formations/domaines");
        List<String> domaines = formationService.findAllDomaines();
        return ResponseEntity.ok(ApiResponse.success(domaines, "Domaines récupérés"));
    }

    // ===== CRUD =====

    @PostMapping
    @Operation(summary = "Crée une nouvelle formation")
    public ResponseEntity<ApiResponse<FormationDTO>> createFormation(
            @Valid @RequestBody FormationDTO dto) {

        log.info("POST /api/formations - Création d'une formation: {}", dto.getTitre());

        FormationDTO created = formationService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Formation créée avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour une formation")
    public ResponseEntity<ApiResponse<FormationDTO>> updateFormation(
            @Parameter(description = "ID de la formation") @PathVariable Long id,
            @Valid @RequestBody FormationDTO dto) {

        log.info("PUT /api/formations/{} - Mise à jour", id);

        FormationDTO updated = formationService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Formation mise à jour avec succès"));
    }

    @PatchMapping("/{id}/activer")
    @Operation(summary = "Active une formation")
    public ResponseEntity<ApiResponse<FormationDTO>> activerFormation(
            @Parameter(description = "ID de la formation") @PathVariable Long id) {

        log.info("PATCH /api/formations/{}/activer", id);

        FormationDTO activee = formationService.activer(id);
        return ResponseEntity.ok(ApiResponse.success(activee, "Formation activée avec succès"));
    }

    @PatchMapping("/{id}/desactiver")
    @Operation(summary = "Désactive une formation")
    public ResponseEntity<ApiResponse<FormationDTO>> desactiverFormation(
            @Parameter(description = "ID de la formation") @PathVariable Long id) {

        log.info("PATCH /api/formations/{}/desactiver", id);

        FormationDTO desactivee = formationService.desactiver(id);
        return ResponseEntity.ok(ApiResponse.success(desactivee, "Formation désactivée avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une formation (désactive)")
    public ResponseEntity<ApiResponse<Void>> deleteFormation(
            @Parameter(description = "ID de la formation") @PathVariable Long id) {

        log.info("DELETE /api/formations/{}", id);

        formationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Formation supprimée avec succès"));
    }

    // ===== GESTION DES PARTICIPANTS =====

    @PostMapping("/{formationId}/participants/{employeId}")
    @Operation(summary = "Ajoute un participant à une formation")
    public ResponseEntity<ApiResponse<FormationDTO>> ajouterParticipant(
            @Parameter(description = "ID de la formation") @PathVariable Long formationId,
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("POST /api/formations/{}/participants/{}", formationId, employeId);

        FormationDTO updated = formationService.ajouterParticipant(formationId, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Participant ajouté avec succès"));
    }

    @DeleteMapping("/{formationId}/participants/{employeId}")
    @Operation(summary = "Retire un participant d'une formation")
    public ResponseEntity<ApiResponse<FormationDTO>> retirerParticipant(
            @Parameter(description = "ID de la formation") @PathVariable Long formationId,
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("DELETE /api/formations/{}/participants/{}", formationId, employeId);

        FormationDTO updated = formationService.retirerParticipant(formationId, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Participant retiré avec succès"));
    }

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère les formations d'un employé")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getFormationsByEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/formations/employe/{}", employeId);

        List<FormationDTO> formations = formationService.findFormationsByEmployeId(employeId);
        return ResponseEntity.ok(ApiResponse.success(formations, "Formations de l'employé récupérées"));
    }

    @GetMapping("/non-suivies/{employeId}")
    @Operation(summary = "Récupère les formations non suivies par un employé")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getFormationsNonSuivies(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/formations/non-suivies/{}", employeId);

        List<FormationDTO> formations = formationService.findFormationsNonSuiviesParEmploye(employeId);
        return ResponseEntity.ok(ApiResponse.success(formations, "Formations non suivies récupérées"));
    }

    @GetMapping("/populaires")
    @Operation(summary = "Récupère les formations les plus populaires")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getFormationsPopulaires(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/formations/populaires?limit={}", limit);

        List<FormationDTO> populaires = formationService.findFormationsPopulaires(limit);
        return ResponseEntity.ok(ApiResponse.success(populaires, "Formations populaires récupérées"));
    }

    // ===== STATISTIQUES =====

    @GetMapping("/stats/domaine")
    @Operation(summary = "Statistiques des formations par domaine")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByDomaine() {
        log.info("GET /api/formations/stats/domaine");
        Map<String, Long> stats = formationService.countByDomaine();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par domaine récupérées"));
    }

    @GetMapping("/stats/duree-moyenne")
    @Operation(summary = "Calcule la durée moyenne des formations")
    public ResponseEntity<ApiResponse<Double>> getDureeMoyenne() {
        log.info("GET /api/formations/stats/duree-moyenne");
        Double moyenne = formationService.calculerDureeMoyenne();
        return ResponseEntity.ok(ApiResponse.success(moyenne, "Durée moyenne calculée"));
    }

    @GetMapping("/stats/tableau-bord")
    @Operation(summary = "Récupère les statistiques pour le tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsTableauBord() {
        log.info("GET /api/formations/stats/tableau-bord");
        Map<String, Object> stats = formationService.getStatsTableauBord();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques tableau de bord récupérées"));
    }

    @GetMapping("/recentes")
    @Operation(summary = "Récupère les formations récentes")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> getFormationsRecentes() {
        log.info("GET /api/formations/recentes");
        List<FormationDTO> recentes = formationService.findFormationsRecentes();
        return ResponseEntity.ok(ApiResponse.success(recentes, "Formations récentes récupérées"));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des formations par mot-clé")
    public ResponseEntity<ApiResponse<List<FormationDTO>>> searchFormations(
            @RequestParam String keyword) {

        log.info("GET /api/formations/search?keyword={}", keyword);

        List<FormationDTO> result = formationService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }
}