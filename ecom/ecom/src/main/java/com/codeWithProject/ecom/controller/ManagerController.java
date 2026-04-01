package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;  // ← AJOUT
import com.codeWithProject.ecom.service.ManagerService;
import com.codeWithProject.ecom.service.dto.ManagerDTO;
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

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Managers", description = "API de gestion des managers")
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping
    @Operation(summary = "Liste tous les managers")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getAllManagers() {
        log.info("GET /api/managers - Récupération de tous les managers");
        List<ManagerDTO> managers = managerService.findAll();
        return ResponseEntity.ok(ApiResponse.success(managers, "Managers récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des managers")
    public ResponseEntity<ApiResponse<Page<ManagerDTO>>> getAllManagersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("GET /api/managers/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ManagerDTO> managers = managerService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(managers, "Managers récupérés avec succès"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un manager par son ID")
    public ResponseEntity<ApiResponse<ManagerDTO>> getManagerById(
            @Parameter(description = "ID du manager") @PathVariable Long id) {

        log.info("GET /api/managers/{}", id);

        return managerService.findById(id)
                .map(manager -> ResponseEntity.ok(ApiResponse.success(manager, "Manager trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère un manager par l'ID de l'employé associé")
    public ResponseEntity<ApiResponse<ManagerDTO>> getManagerByEmployeId(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("GET /api/managers/employe/{}", employeId);

        return managerService.findByEmployeId(employeId)
                .map(manager -> ResponseEntity.ok(ApiResponse.success(manager, "Manager trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departement/{departement}")
    @Operation(summary = "Récupère les managers par département")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getManagersByDepartement(
            @Parameter(description = "Nom du département") @PathVariable String departement) {

        log.info("GET /api/managers/departement/{}", departement);

        List<ManagerDTO> managers = managerService.findByDepartement(departement);
        return ResponseEntity.ok(ApiResponse.success(managers, "Managers par département récupérés"));
    }

    @GetMapping("/actifs")
    @Operation(summary = "Récupère les managers actifs")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getManagersActifs() {
        log.info("GET /api/managers/actifs");
        List<ManagerDTO> actifs = managerService.findManagersActifs();
        return ResponseEntity.ok(ApiResponse.success(actifs, "Managers actifs récupérés"));
    }

    @GetMapping("/sans-equipe")
    @Operation(summary = "Récupère les managers sans équipe")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> getManagersSansEquipe() {
        log.info("GET /api/managers/sans-equipe");
        List<ManagerDTO> sansEquipe = managerService.findManagersSansEquipe();
        return ResponseEntity.ok(ApiResponse.success(sansEquipe, "Managers sans équipe récupérés"));
    }

    @PostMapping
    @Operation(summary = "Crée un nouveau manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> createManager(
            @Valid @RequestBody ManagerDTO dto) {

        log.info("POST /api/managers - Création d'un manager pour le département: {}", dto.getDepartement());

        ManagerDTO created = managerService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Manager créé avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> updateManager(
            @Parameter(description = "ID du manager") @PathVariable Long id,
            @Valid @RequestBody ManagerDTO dto) {

        log.info("PUT /api/managers/{} - Mise à jour", id);

        ManagerDTO updated = managerService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Manager mis à jour avec succès"));
    }

    @PatchMapping("/{id}/activer")
    @Operation(summary = "Active un manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> activerManager(
            @Parameter(description = "ID du manager") @PathVariable Long id) {

        log.info("PATCH /api/managers/{}/activer", id);

        ManagerDTO active = managerService.activer(id);
        return ResponseEntity.ok(ApiResponse.success(active, "Manager activé avec succès"));
    }

    @PatchMapping("/{id}/desactiver")
    @Operation(summary = "Désactive un manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> desactiverManager(
            @Parameter(description = "ID du manager") @PathVariable Long id) {

        log.info("PATCH /api/managers/{}/desactiver", id);

        ManagerDTO desactive = managerService.desactiver(id);
        return ResponseEntity.ok(ApiResponse.success(desactive, "Manager désactivé avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un manager")
    public ResponseEntity<ApiResponse<Void>> deleteManager(
            @Parameter(description = "ID du manager") @PathVariable Long id) {

        log.info("DELETE /api/managers/{}", id);

        managerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Manager supprimé avec succès"));
    }

    @PostMapping("/{managerId}/employes/{employeId}")
    @Operation(summary = "Ajoute un employé à l'équipe du manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> ajouterEmploye(
            @Parameter(description = "ID du manager") @PathVariable Long managerId,
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("POST /api/managers/{}/employes/{}", managerId, employeId);

        ManagerDTO updated = managerService.ajouterEmploye(managerId, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employé ajouté à l'équipe avec succès"));
    }

    @DeleteMapping("/{managerId}/employes/{employeId}")
    @Operation(summary = "Retire un employé de l'équipe du manager")
    public ResponseEntity<ApiResponse<ManagerDTO>> retirerEmploye(
            @Parameter(description = "ID du manager") @PathVariable Long managerId,
            @Parameter(description = "ID de l'employé") @PathVariable Long employeId) {

        log.info("DELETE /api/managers/{}/employes/{}", managerId, employeId);

        ManagerDTO updated = managerService.retirerEmploye(managerId, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employé retiré de l'équipe avec succès"));
    }

    @GetMapping("/{id}/rapport-equipe")
    @Operation(summary = "Génère un rapport d'équipe pour un manager")
    public ResponseEntity<ApiResponse<String>> getRapportEquipe(
            @Parameter(description = "ID du manager") @PathVariable Long id) {

        log.info("GET /api/managers/{}/rapport-equipe", id);

        String rapport = managerService.genererRapportEquipe(id);
        return ResponseEntity.ok(ApiResponse.success(rapport, "Rapport d'équipe généré"));
    }

    @GetMapping("/stats/globales")
    @Operation(summary = "Récupère les statistiques globales des managers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getManagersStats() {
        log.info("GET /api/managers/stats/globales");
        Map<String, Object> stats = managerService.getManagersStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques des managers récupérées"));
    }

    @GetMapping("/stats/departement")
    @Operation(summary = "Compte les managers par département")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countByDepartement() {
        log.info("GET /api/managers/stats/departement");
        Map<String, Long> stats = managerService.countByDepartement();
        return ResponseEntity.ok(ApiResponse.success(stats, "Managers par département comptés"));
    }

    @GetMapping("/stats/anciennete-moyenne")
    @Operation(summary = "Calcule l'ancienneté moyenne des managers")
    public ResponseEntity<ApiResponse<Double>> getAncienneteMoyenne() {
        log.info("GET /api/managers/stats/anciennete-moyenne");
        Double moyenne = managerService.calculerAncienneteMoyenne();
        return ResponseEntity.ok(ApiResponse.success(moyenne, "Ancienneté moyenne calculée"));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des managers par mot-clé")
    public ResponseEntity<ApiResponse<List<ManagerDTO>>> searchManagers(
            @RequestParam String keyword) {

        log.info("GET /api/managers/search?keyword={}", keyword);

        List<ManagerDTO> result = managerService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }
}