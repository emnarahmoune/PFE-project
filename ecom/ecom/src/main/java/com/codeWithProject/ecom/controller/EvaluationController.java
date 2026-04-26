package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.EvaluationService;
import com.codeWithProject.ecom.service.dto.EvaluationDTO;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Évaluations", description = "Gestion des évaluations de performance")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping
    @Operation(summary = "Liste toutes les évaluations")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getAll() {
        List<EvaluationDTO> list = evaluationService.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Évaluations récupérées"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée")
    public ResponseEntity<ApiResponse<Page<EvaluationDTO>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateEvaluation") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EvaluationDTO> evaluations = evaluationService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(evaluations, "Évaluations récupérées"));
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
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getByEmploye(@PathVariable Long employeId) {
        List<EvaluationDTO> list = evaluationService.findByEmployeId(employeId);
        return ResponseEntity.ok(ApiResponse.success(list, "Évaluations de l'employé récupérées"));
    }

    @PostMapping
    @Operation(summary = "Crée une nouvelle évaluation")
    public ResponseEntity<ApiResponse<EvaluationDTO>> create(@Valid @RequestBody EvaluationDTO dto) {
        EvaluationDTO created = evaluationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Évaluation créée avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour une évaluation")
    public ResponseEntity<ApiResponse<EvaluationDTO>> update(@PathVariable Long id, @Valid @RequestBody EvaluationDTO dto) {
        EvaluationDTO updated = evaluationService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Évaluation mise à jour"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une évaluation")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        evaluationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Évaluation supprimée"));
    }

    @GetMapping("/stats/moyenne/{employeId}")
    @Operation(summary = "Moyenne des notes d'un employé sur l'année en cours")
    public ResponseEntity<ApiResponse<Double>> getMoyenneAnnuelle(@PathVariable Long employeId) {
        int annee = java.time.LocalDate.now().getYear();
        Double moyenne = evaluationService.getMoyenneNotes(employeId, annee);
        return ResponseEntity.ok(ApiResponse.success(moyenne != null ? moyenne : 0.0, "Moyenne calculée"));
    }
}