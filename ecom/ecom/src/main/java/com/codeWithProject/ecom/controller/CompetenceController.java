package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;  // ← AJOUT
import com.codeWithProject.ecom.entity.Competence;
import com.codeWithProject.ecom.repository.CompetenceRepository;
import com.codeWithProject.ecom.service.CompetenceService;
import com.codeWithProject.ecom.service.dto.CompetenceDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;

/**
 * Contrôleur REST pour la gestion des compétences
 * Endpoints : /api/competences
 */
@RestController
@RequestMapping("/api/competences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Compétences", description = "API de gestion des compétences")
public class CompetenceController {

    private final CompetenceService competenceService;
    private final CompetenceRepository competenceRepository;

    /**
     * Récupère toutes les compétences
     */
    @GetMapping
    @Operation(summary = "Liste toutes les compétences")
    public ResponseEntity<ApiResponse<List<CompetenceDTO>>> getAllCompetences() {
        log.info("GET /api/competences - Récupération de toutes les compétences");
        List<CompetenceDTO> competences = competenceService.findAll();
        return ResponseEntity.ok(ApiResponse.success(competences, "Compétences récupérées avec succès"));
    }

    /**
     * Récupère toutes les compétences avec pagination
     */
    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des compétences")
    public ResponseEntity<ApiResponse<Page<CompetenceDTO>>> getAllCompetencesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("GET /api/competences/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CompetenceDTO> competences = competenceService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(competences, "Compétences récupérées avec succès"));
    }

    /**
     * Récupère une compétence par son ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère une compétence par son ID")
    public ResponseEntity<ApiResponse<CompetenceDTO>> getCompetenceById(
            @Parameter(description = "ID de la compétence") @PathVariable Long id) {

        log.info("GET /api/competences/{}", id);

        return competenceService.findById(id)
                .map(comp -> ResponseEntity.ok(ApiResponse.success(comp, "Compétence trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère une compétence par son nom
     */
    @GetMapping("/nom/{nom}")
    @Operation(summary = "Récupère une compétence par son nom")
    public ResponseEntity<ApiResponse<CompetenceDTO>> getCompetenceByNom(
            @Parameter(description = "Nom de la compétence") @PathVariable String nom) {

        log.info("GET /api/competences/nom/{}", nom);

        return competenceService.findByNom(nom)
                .map(comp -> ResponseEntity.ok(ApiResponse.success(comp, "Compétence trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère les compétences par catégorie
     */
    @GetMapping("/categorie/{categorie}")
    @Operation(summary = "Récupère les compétences par catégorie")
    public ResponseEntity<ApiResponse<List<CompetenceDTO>>> getCompetencesByCategorie(
            @Parameter(description = "Catégorie") @PathVariable String categorie) {

        log.info("GET /api/competences/categorie/{}", categorie);

        List<CompetenceDTO> competences = competenceService.findByCategorie(categorie);
        return ResponseEntity.ok(ApiResponse.success(competences, "Compétences par catégorie récupérées"));
    }

    /**
     * Récupère toutes les catégories
     */
    @GetMapping("/categories")
    @Operation(summary = "Récupère toutes les catégories de compétences")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        log.info("GET /api/competences/categories");
        List<String> categories = competenceService.findAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Catégories récupérées"));
    }

    /**
     * Crée une nouvelle compétence
     */
    @PostMapping
    @Operation(summary = "Crée une nouvelle compétence")
    public ResponseEntity<ApiResponse<CompetenceDTO>> createCompetence(
            @Valid @RequestBody CompetenceDTO dto) {

        log.info("POST /api/competences - Création d'une compétence: {}", dto.getNom());

        CompetenceDTO created = competenceService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Compétence créée avec succès"));
    }

    /**
     * Met à jour une compétence
     */
    @PutMapping("/{id}")
    @Operation(summary = "Met à jour une compétence")
    public ResponseEntity<ApiResponse<CompetenceDTO>> updateCompetence(
            @Parameter(description = "ID de la compétence") @PathVariable Long id,
            @Valid @RequestBody CompetenceDTO dto) {

        log.info("PUT /api/competences/{} - Mise à jour", id);

        CompetenceDTO updated = competenceService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Compétence mise à jour avec succès"));
    }

    /**
     * Supprime une compétence
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une compétence")
    public ResponseEntity<ApiResponse<Void>> deleteCompetence(
            @Parameter(description = "ID de la compétence") @PathVariable Long id) {

        log.info("DELETE /api/competences/{}", id);

        competenceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Compétence supprimée avec succès"));
    }

    /**
     * Vérifie si un nom de compétence existe
     */
    @GetMapping("/exists/{nom}")
    @Operation(summary = "Vérifie si un nom de compétence existe")
    public ResponseEntity<ApiResponse<Boolean>> checkNomExists(
            @Parameter(description = "Nom à vérifier") @PathVariable String nom) {

        log.info("GET /api/competences/exists/{}", nom);

        boolean exists = competenceService.existsByNom(nom);
        return ResponseEntity.ok(ApiResponse.success(exists, "Vérification effectuée"));
    }

    /**
     * Compte le nombre total de compétences
     */
    @GetMapping("/count")
    @Operation(summary = "Compte le nombre total de compétences")
    public ResponseEntity<ApiResponse<Long>> countCompetences() {
        log.info("GET /api/competences/count");
        long count = competenceService.count();
        return ResponseEntity.ok(ApiResponse.success(count, "Nombre de compétences récupéré"));
    }

    /**
     * Récupère les statistiques par catégorie
     */
    @GetMapping("/stats/categories")
    @Operation(summary = "Statistiques des compétences par catégorie")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByCategorie() {
        log.info("GET /api/competences/stats/categories");
        Map<String, Long> stats = competenceService.getStatsByCategorie();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées"));
    }

    /**
     * Recherche des compétences par mot-clé
     */
    @GetMapping("/search")
    @Operation(summary = "Recherche des compétences par mot-clé")
    public ResponseEntity<ApiResponse<List<CompetenceDTO>>> searchCompetences(
            @Parameter(description = "Mot-clé de recherche") @RequestParam String keyword) {

        log.info("GET /api/competences/search?keyword={}", keyword);

        List<CompetenceDTO> result = competenceService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }

    /**
     * Récupère les compétences les plus utilisées
     */
    @GetMapping("/top/{limit}")
    @Operation(summary = "Récupère les compétences les plus utilisées")
    public ResponseEntity<ApiResponse<List<CompetenceDTO>>> getTopCompetences(
            @Parameter(description = "Nombre maximum de résultats") @PathVariable int limit) {

        log.info("GET /api/competences/top/{}", limit);

        List<CompetenceDTO> top = competenceService.findTopCompetences(limit);
        return ResponseEntity.ok(ApiResponse.success(top, "Top compétences récupérées"));
    }



     @GetMapping("/competences")
    public List<Map<String, Object>> getAll() {

        List<Competence> list = competenceRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Competence c : list) {

            Map<String, Object> map = new HashMap<>();

            map.put("id", c.getId());
            map.put("nom", c.getNom());

            result.add(map);
        }

        return result;
    }


    /**
     * Récupère les compétences non attribuées
     */
    @GetMapping("/non-attribuees")
    @Operation(summary = "Récupère les compétences non attribuées à aucun employé")
    public ResponseEntity<ApiResponse<List<CompetenceDTO>>> getCompetencesNonAttribuees() {
        log.info("GET /api/competences/non-attribuees");
        List<CompetenceDTO> nonAttribuees = competenceService.findCompetencesNonAttribuees();
        return ResponseEntity.ok(ApiResponse.success(nonAttribuees, "Compétences non attribuées récupérées"));
    }
}