package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.EmployeService;
import com.codeWithProject.ecom.service.dto.*;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employés", description = "API de gestion des employés")
public class EmployeController {

    private final EmployeService employeService;

    // ===== RECHERCHES GÉNÉRALES =====

    @GetMapping
    @Operation(summary = "Liste tous les employés")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getAllEmployes() {
        log.info("GET /api/employes - Récupération de tous les employés");
        List<EmployeDTO> employes = employeService.findAll();
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés récupérés avec succès"));
    }

    @GetMapping("/paged")
    @Operation(summary = "Liste paginée des employés")
    public ResponseEntity<ApiResponse<Page<EmployeDTO>>> getAllEmployesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("GET /api/employes/paged - page: {}, size: {}", page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EmployeDTO> employes = employeService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés récupérés avec succès"));
    }

    // ===== RECHERCHES PAR IDENTIFIANTS =====

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un employé par son ID")
    public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeById(
            @Parameter(description = "ID de l'employé") @PathVariable Long id) {

        log.info("GET /api/employes/{}", id);

        return employeService.findById(id)
                .map(employe -> ResponseEntity.ok(ApiResponse.success(employe, "Employé trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/matricule/{matricule}")
    @Operation(summary = "Récupère un employé par son matricule")
    public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeByMatricule(
            @Parameter(description = "Matricule de l'employé") @PathVariable String matricule) {

        log.info("GET /api/employes/matricule/{}", matricule);

        return employeService.findByMatricule(matricule)
                .map(employe -> ResponseEntity.ok(ApiResponse.success(employe, "Employé trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== RECHERCHES PAR ATTRIBUTS =====

    @GetMapping("/departement/{departement}")
    @Operation(summary = "Récupère les employés par département")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByDepartement(
            @Parameter(description = "Nom du département") @PathVariable String departement) {

        log.info("GET /api/employes/departement/{}", departement);

        List<EmployeDTO> employes = employeService.findByDepartement(departement);
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés du département récupérés"));
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Récupère les employés par statut")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByStatut(
            @Parameter(description = "Statut (ACTIF, INACTIF, CONGE)") @PathVariable String statut) {

        log.info("GET /api/employes/statut/{}", statut);

        List<EmployeDTO> employes = employeService.findByStatut(statut);
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés par statut récupérés"));
    }

    @GetMapping("/actifs")
    @Operation(summary = "Récupère les employés actifs")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesActifs() {
        log.info("GET /api/employes/actifs");
        List<EmployeDTO> actifs = employeService.findActifs();
        return ResponseEntity.ok(ApiResponse.success(actifs, "Employés actifs récupérés"));
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Récupère les employés d'un manager")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByManager(
            @Parameter(description = "ID du manager") @PathVariable Long managerId) {

        log.info("GET /api/employes/manager/{}", managerId);

        List<EmployeDTO> employes = employeService.findByManagerId(managerId);
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés du manager récupérés"));
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Récupère les employés d'un service")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByService(
            @Parameter(description = "ID du service") @PathVariable Long serviceId) {

        log.info("GET /api/employes/service/{}", serviceId);

        List<EmployeDTO> employes = employeService.findByServiceId(serviceId);
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés du service récupérés"));
    }

    // ===== GESTION DES CONGÉS =====

    @GetMapping("/solde-conges-faible/{seuil}")
    @Operation(summary = "Récupère les employés avec solde de congés faible")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesSoldeCongesFaible(
            @Parameter(description = "Seuil de congés") @PathVariable Integer seuil) {

        log.info("GET /api/employes/solde-conges-faible/{}", seuil);

        List<EmployeDTO> employes = employeService.findSoldeCongesFaible(seuil);
        return ResponseEntity.ok(ApiResponse.success(employes, "Employés avec solde faible récupérés"));
    }

    // ===== CRUD =====

    @PostMapping
    @Operation(summary = "Crée un nouvel employé")
    public ResponseEntity<ApiResponse<EmployeDTO>> createEmploye(
            @Valid @RequestBody EmployeDTO dto) {

        log.info("POST /api/employes - Création d'un employé: {}", dto.getMatricule());

        EmployeDTO created = employeService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Employé créé avec succès"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un employé")
    public ResponseEntity<ApiResponse<EmployeDTO>> updateEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long id,
            @Valid @RequestBody EmployeDTO dto) {

        log.info("PUT /api/employes/{} - Mise à jour", id);

        EmployeDTO updated = employeService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Employé mis à jour avec succès"));
    }

    @PatchMapping("/{id}/profil")
    @Operation(summary = "Met à jour le profil d'un employé (poste, salaire, département)")
    public ResponseEntity<ApiResponse<EmployeDTO>> mettreAJourProfil(
            @Parameter(description = "ID de l'employé") @PathVariable Long id,
            @RequestParam(required = false) String poste,
            @RequestParam(required = false) Double salaire,
            @RequestParam(required = false) String departement) {

        log.info("PATCH /api/employes/{}/profil", id);

        EmployeDTO updated = employeService.mettreAJourProfil(id, poste, salaire, departement);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profil mis à jour avec succès"));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Change le statut d'un employé")
    public ResponseEntity<ApiResponse<EmployeDTO>> changerStatut(
            @Parameter(description = "ID de l'employé") @PathVariable Long id,
            @RequestParam String statut) {

        log.info("PATCH /api/employes/{}/statut - nouveau statut: {}", id, statut);

        EmployeDTO updated = employeService.changerStatut(id, statut);
        return ResponseEntity.ok(ApiResponse.success(updated, "Statut changé avec succès"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un employé")
    public ResponseEntity<ApiResponse<Void>> deleteEmploye(
            @Parameter(description = "ID de l'employé") @PathVariable Long id) {

        log.info("DELETE /api/employes/{}", id);

        employeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Employé supprimé avec succès"));
    }

    // ===== STATISTIQUES =====

    @GetMapping("/count")
    @Operation(summary = "Compte le nombre total d'employés")
    public ResponseEntity<ApiResponse<Long>> countEmployes() {
        log.info("GET /api/employes/count");
        return ResponseEntity.ok(ApiResponse.success(employeService.count(), "Nombre d'employés récupéré"));
    }

    @GetMapping("/stats/departement")
    @Operation(summary = "Statistiques des employés par département")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByDepartement() {
        log.info("GET /api/employes/stats/departement");
        Map<String, Long> stats = employeService.countByDepartement();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par département récupérées"));
    }

    @GetMapping("/stats/statut")
    @Operation(summary = "Statistiques des employés par statut")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        log.info("GET /api/employes/stats/statut");
        Map<String, Long> stats = employeService.countByStatut();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques par statut récupérées"));
    }

    @GetMapping("/stats/masse-salariale")
    @Operation(summary = "Calcule la masse salariale totale")
    public ResponseEntity<ApiResponse<Double>> getMasseSalariale() {
        log.info("GET /api/employes/stats/masse-salariale");
        Double masse = employeService.calculerMasseSalariale();
        return ResponseEntity.ok(ApiResponse.success(masse, "Masse salariale calculée"));
    }

    @GetMapping("/stats/salaire-moyen")
    @Operation(summary = "Calcule le salaire moyen")
    public ResponseEntity<ApiResponse<Double>> getSalaireMoyen() {
        log.info("GET /api/employes/stats/salaire-moyen");
        Double moyen = employeService.calculerSalaireMoyen();
        return ResponseEntity.ok(ApiResponse.success(moyen, "Salaire moyen calculé"));
    }

    @GetMapping("/stats/tableau-bord")
    @Operation(summary = "Récupère les statistiques pour le tableau de bord")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsTableauBord() {
        log.info("GET /api/employes/stats/tableau-bord");
        Map<String, Object> stats = employeService.getStatsTableauBord();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques tableau de bord récupérées"));
    }

    @GetMapping("/recents")
    @Operation(summary = "Récupère les employés récents")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesRecents(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/employes/recents?limit={}", limit);

        List<EmployeDTO> recents = employeService.findEmployesRecents(limit);
        return ResponseEntity.ok(ApiResponse.success(recents, "Employés récents récupérés"));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des employés par mot-clé")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> searchEmployes(
            @RequestParam String keyword) {

        log.info("GET /api/employes/search?keyword={}", keyword);

        List<EmployeDTO> result = employeService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultats de la recherche"));
    }

    // ===== NOUVEAUX ENDPOINTS POUR L'ESPACE EMPLOYÉ =====

    @GetMapping("/mon-profil")
    @Operation(summary = "Récupère le profil de l'employé connecté")
    public ResponseEntity<ApiResponse<EmployeDTO>> getMonProfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/employes/mon-profil - Récupération du profil employé");

        String email = userDetails.getUsername();
        EmployeDTO employe = employeService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return ResponseEntity.ok(ApiResponse.success(employe, "Profil récupéré avec succès"));
    }

    @GetMapping("/mon-solde-conges")
    @Operation(summary = "Récupère le solde de congés de l'employé connecté")
    public ResponseEntity<ApiResponse<SoldeCongesDTO>> getMonSoldeConges(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/employes/mon-solde-conges");

        String email = userDetails.getUsername();
        SoldeCongesDTO solde = employeService.getSoldeCongesByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(solde, "Solde de congés récupéré"));
    }

    @GetMapping("/mes-competences")
    @Operation(summary = "Récupère les compétences de l'employé connecté")
    public ResponseEntity<ApiResponse<List<CompetenceEmployeDTO>>> getMesCompetences(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/employes/mes-competences");

        String email = userDetails.getUsername();
        List<CompetenceEmployeDTO> competences = employeService.getCompetencesByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(competences, "Compétences récupérées"));
    }

    @GetMapping("/mes-formations")
    @Operation(summary = "Récupère les formations de l'employé connecté")
    public ResponseEntity<ApiResponse<List<FormationEmployeDTO>>> getMesFormations(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/employes/mes-formations");

        String email = userDetails.getUsername();
        List<FormationEmployeDTO> formations = employeService.getFormationsByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(formations, "Formations récupérées"));
    }

    @GetMapping("/mon-historique-conges")
    @Operation(summary = "Récupère l'historique des congés de l'employé connecté")
    public ResponseEntity<ApiResponse<List<HistoriqueCongeDTO>>> getMonHistoriqueConges(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/employes/mon-historique-conges");

        String email = userDetails.getUsername();
        List<HistoriqueCongeDTO> historique = employeService.getHistoriqueCongesByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(historique, "Historique récupéré"));
    }

    @PatchMapping("/mon-profil")
    @Operation(summary = "Met à jour les informations personnelles de l'employé connecté")
    public ResponseEntity<ApiResponse<EmployeDTO>> updateMonProfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfilRequest request) {
        log.info("PATCH /api/employes/mon-profil - Mise à jour du profil");

        String email = userDetails.getUsername();
        EmployeDTO updated = employeService.updateProfilByEmail(email, request);

        return ResponseEntity.ok(ApiResponse.success(updated, "Profil mis à jour avec succès"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change le mot de passe de l'employé connecté")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("POST /api/employes/change-password - Changement de mot de passe");

        String email = userDetails.getUsername();
        employeService.changePasswordByEmail(email, request);

        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès"));
    }

    @PatchMapping("/change-email")
    @Operation(summary = "Change l'email de l'employé connecté")
    public ResponseEntity<ApiResponse<EmployeDTO>> changeEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String newEmail) {
        log.info("PATCH /api/employes/change-email - Nouvel email: {}", newEmail);

        String email = userDetails.getUsername();
        EmployeDTO updated = employeService.changeEmailByEmail(email, newEmail);

        return ResponseEntity.ok(ApiResponse.success(updated, "Email changé avec succès"));
    }
}