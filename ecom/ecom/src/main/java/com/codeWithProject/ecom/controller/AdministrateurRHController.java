package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.AdministrateurRHService;
import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/administrateurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administrateurs RH", description = "API de gestion des administrateurs RH")
@PreAuthorize("hasRole('ADMIN_RH')")
public class AdministrateurRHController {

    private final AdministrateurRHService administrateurRHService;

    @GetMapping
    @Operation(summary = "Liste tous les administrateurs")
    public ResponseEntity<ApiResponse<List<AdministrateurRHDTO>>> getAllAdministrateurs() {
        log.info("GET /api/admin/administrateurs");
        List<AdministrateurRHDTO> administrateurs = administrateurRHService.findAll();
        return ResponseEntity.ok(ApiResponse.success(administrateurs, "Administrateurs récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des administrateurs")
    public ResponseEntity<ApiResponse<Page<AdministrateurRHDTO>>> getAllAdministrateursPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("GET /api/admin/administrateurs/paged - page: {}, size: {}", page, size);
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AdministrateurRHDTO> administrateurs = administrateurRHService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(administrateurs, "Administrateurs récupérés avec succès"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un administrateur par son ID")
    public ResponseEntity<ApiResponse<AdministrateurRHDTO>> getAdministrateurById(@PathVariable Long id) {
        log.info("GET /api/admin/administrateurs/{}", id);
        return administrateurRHService.findById(id)
                .map(admin -> ResponseEntity.ok(ApiResponse.success(admin, "Administrateur trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employe/{employeId}")
    @Operation(summary = "Récupère un administrateur par l'ID de l'employé associé")
    public ResponseEntity<ApiResponse<AdministrateurRHDTO>> getAdministrateurByEmployeId(@PathVariable Long employeId) {
        log.info("GET /api/admin/administrateurs/employe/{}", employeId);
        return administrateurRHService.findByEmployeId(employeId)
                .map(admin -> ResponseEntity.ok(ApiResponse.success(admin, "Administrateur trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Récupère un administrateur par son email")
    public ResponseEntity<ApiResponse<AdministrateurRHDTO>> getAdministrateurByEmail(@PathVariable String email) {
        log.info("GET /api/admin/administrateurs/email/{}", email);
        return administrateurRHService.findByEmail(email)
                .map(admin -> ResponseEntity.ok(ApiResponse.success(admin, "Administrateur trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crée un nouvel administrateur")
    public ResponseEntity<ApiResponse<AdministrateurRHDTO>> createAdministrateur(@Valid @RequestBody AdministrateurRHDTO dto) {
        log.info("POST /api/admin/administrateurs");
        AdministrateurRHDTO created = administrateurRHService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Administrateur créé avec succès"));
    }

    @PutMapping("/{id}/assigner-employe/{employeId}")
    @Operation(summary = "Assigne un employé à un administrateur")
    public ResponseEntity<ApiResponse<AdministrateurRHDTO>> assignerEmploye(@PathVariable Long id, @PathVariable Long employeId) {
        log.info("PUT /api/admin/administrateurs/{}/assigner-employe/{}", id, employeId);
        AdministrateurRHDTO updated = administrateurRHService.assignerEmploye(id, employeId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employé assigné avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un administrateur")
    public ResponseEntity<ApiResponse<String>> deleteAdministrateur(@PathVariable Long id) {
        log.info("DELETE /api/admin/administrateurs/{}", id);
        administrateurRHService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Administrateur supprimé avec succès"));
    }

    @GetMapping("/count")
    @Operation(summary = "Compte le nombre total d'administrateurs")
    public ResponseEntity<ApiResponse<Long>> countAdministrateurs() {
        log.info("GET /api/admin/administrateurs/count");
        long count = administrateurRHService.count();
        return ResponseEntity.ok(ApiResponse.success(count, "Nombre d'administrateurs récupéré"));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des administrateurs par mot-clé")
    public ResponseEntity<ApiResponse<List<AdministrateurRHDTO>>> searchAdministrateurs(@RequestParam String keyword) {
        log.info("GET /api/admin/administrateurs/search?keyword={}", keyword);
        List<AdministrateurRHDTO> result = administrateurRHService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }
}