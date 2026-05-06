package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.EvaluationService;
import com.codeWithProject.ecom.service.dto.EvaluationDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
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

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Évaluations", description = "Gestion des évaluations de performance")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final EmployeRepository employeRepository;

    // ==========================================================
    // ADMIN RH
    // ==========================================================

    @GetMapping("/admin")
    @Operation(summary = "Admin RH - Liste toutes les évaluations")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getAllForAdmin() {
        List<EvaluationDTO> list = evaluationService.findAllForAdminRh();

        return ResponseEntity.ok(
                ApiResponse.success(list, "Évaluations récupérées")
        );
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Admin RH - Statistiques globales des évaluations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        evaluationService.getStatsGlobales(),
                        "Statistiques globales récupérées"
                )
        );
    }

    @PostMapping("/admin/managers")
@Operation(summary = "Admin RH - Évalue un manager")
public ResponseEntity<ApiResponse<EvaluationDTO>> createManagerEvaluationByAdmin(
        @Valid @RequestBody EvaluationDTO dto,
        Authentication authentication
) {
    Employe adminRh = getCurrentEmploye(authentication);

    EvaluationDTO created = evaluationService.createByAdminRhForManager(dto, adminRh.getId());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(created, "Manager évalué avec succès"));
}

    // ==========================================================
    // MANAGER
    // ==========================================================

    @GetMapping("/manager")
    @Operation(summary = "Manager - Liste les évaluations de son équipe")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getManagerEvaluations(
            Authentication authentication
    ) {
        Employe manager = getCurrentEmploye(authentication);

        List<EvaluationDTO> list = evaluationService.findByManagerId(manager.getId());

        return ResponseEntity.ok(
                ApiResponse.success(list, "Évaluations de l'équipe récupérées")
        );
    }

    @GetMapping("/manager/stats")
    @Operation(summary = "Manager - Statistiques des évaluations de son équipe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getManagerStats(
            Authentication authentication
    ) {
        Employe manager = getCurrentEmploye(authentication);

        return ResponseEntity.ok(
                ApiResponse.success(
                        evaluationService.getStatsManager(manager.getId()),
                        "Statistiques manager récupérées"
                )
        );
    }

    @PostMapping("/manager")
    @Operation(summary = "Manager - Crée une évaluation pour un membre de son équipe")
    public ResponseEntity<ApiResponse<EvaluationDTO>> createByManager(
            @Valid @RequestBody EvaluationDTO dto,
            Authentication authentication
    ) {
        Employe manager = getCurrentEmploye(authentication);

        EvaluationDTO created = evaluationService.createByManager(dto, manager.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Évaluation créée avec succès"));
    }

    @PutMapping("/manager/{id}")
    @Operation(summary = "Manager - Met à jour une évaluation qu'il a créée")
    public ResponseEntity<ApiResponse<EvaluationDTO>> updateByManager(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationDTO dto,
            Authentication authentication
    ) {
        Employe manager = getCurrentEmploye(authentication);

        EvaluationDTO updated = evaluationService.updateByManager(id, dto, manager.getId());

        return ResponseEntity.ok(
                ApiResponse.success(updated, "Évaluation mise à jour")
        );
    }

    @DeleteMapping("/manager/{id}")
    @Operation(summary = "Manager - Supprime une évaluation qu'il a créée")
    public ResponseEntity<ApiResponse<Void>> deleteByManager(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Employe manager = getCurrentEmploye(authentication);

        evaluationService.deleteByManager(id, manager.getId());

        return ResponseEntity.ok(
                ApiResponse.success(null, "Évaluation supprimée")
        );
    }

    // ==========================================================
    // EMPLOYÉ CONNECTÉ
    // ==========================================================

    @GetMapping("/me")
    @Operation(summary = "Employé - Consulte ses évaluations")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getMyEvaluations(
            Authentication authentication
    ) {
        Employe employe = getCurrentEmploye(authentication);

        List<EvaluationDTO> list = evaluationService.findByEmployeId(employe.getId());

        return ResponseEntity.ok(
                ApiResponse.success(list, "Mes évaluations récupérées")
        );
    }

    @GetMapping("/me/stats")
    @Operation(summary = "Employé - Consulte ses statistiques d'évaluation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyStats(
            Authentication authentication
    ) {
        Employe employe = getCurrentEmploye(authentication);

        return ResponseEntity.ok(
                ApiResponse.success(
                        evaluationService.getStatsEmploye(employe.getId()),
                        "Mes statistiques récupérées"
                )
        );
    }

    // ==========================================================
    // ENDPOINTS COMPATIBILITÉ / ADMIN
    // ==========================================================

    @GetMapping
    @Operation(summary = "Liste toutes les évaluations")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getAll() {
        List<EvaluationDTO> list = evaluationService.findAll();

        return ResponseEntity.ok(
                ApiResponse.success(list, "Évaluations récupérées")
        );
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée")
    public ResponseEntity<ApiResponse<Page<EvaluationDTO>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateEvaluation") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EvaluationDTO> evaluations = evaluationService.findAll(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(evaluations, "Évaluations récupérées")
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère une évaluation par son ID")
    public ResponseEntity<ApiResponse<EvaluationDTO>> getById(@PathVariable Long id) {
        return evaluationService.findById(id)
                .map(eval -> ResponseEntity.ok(ApiResponse.success(eval, "Évaluation trouvée")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère toutes les évaluations d'un employé")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getByEmploye(
            @PathVariable Long employeId
    ) {
        List<EvaluationDTO> list = evaluationService.findByEmployeId(employeId);

        return ResponseEntity.ok(
                ApiResponse.success(list, "Évaluations de l'employé récupérées")
        );
    }

    @PostMapping
    @Operation(summary = "Crée une nouvelle évaluation - Admin/RH")
    public ResponseEntity<ApiResponse<EvaluationDTO>> create(
            @Valid @RequestBody EvaluationDTO dto
    ) {
        EvaluationDTO created = evaluationService.create(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Évaluation créée avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour une évaluation - Admin/RH")
    public ResponseEntity<ApiResponse<EvaluationDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationDTO dto
    ) {
        EvaluationDTO updated = evaluationService.update(id, dto);

        return ResponseEntity.ok(
                ApiResponse.success(updated, "Évaluation mise à jour")
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une évaluation - Admin/RH")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        evaluationService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Évaluation supprimée")
        );
    }

    @GetMapping("/stats/moyenne/{employeId}")
    @Operation(summary = "Moyenne des notes d'un employé sur l'année en cours")
    public ResponseEntity<ApiResponse<Double>> getMoyenneAnnuelle(
            @PathVariable Long employeId
    ) {
        int annee = java.time.LocalDate.now().getYear();

        Double moyenne = evaluationService.getMoyenneNotes(employeId, annee);

        return ResponseEntity.ok(
                ApiResponse.success(
                        moyenne != null ? moyenne : 0.0,
                        "Moyenne calculée"
                )
        );
    }

    @GetMapping("/stats/employe/{employeId}")
    @Operation(summary = "Statistiques d'évaluation d'un employé")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsEmploye(
            @PathVariable Long employeId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        evaluationService.getStatsEmploye(employeId),
                        "Statistiques employé récupérées"
                )
        );
    }

    // ==========================================================
    // HELPERS
    // ==========================================================

    private Employe getCurrentEmploye(Authentication authentication) {
        String email = extractEmail(authentication);

        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Utilisateur connecté introuvable");
        }

        return employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Employé connecté introuvable : " + email));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");

            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("preferred_username");
            }

            if (email == null || email.isBlank()) {
                email = jwt.getSubject();
            }

            return email != null ? email.trim().toLowerCase() : null;
        }

        String name = authentication.getName();

        return name != null ? name.trim().toLowerCase() : null;
    }
}