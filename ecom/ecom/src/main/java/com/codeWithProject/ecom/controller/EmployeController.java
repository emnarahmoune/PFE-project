package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeCompetence;
import com.codeWithProject.ecom.repository.EmployeCompetenceRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employés", description = "API de gestion des employés")
public class EmployeController {

    private final EmployeService employeService;
    private final EmployeRepository employeRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;

    // ===== LISTES GÉNÉRALES =====
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getAllEmployes() {
        return ResponseEntity.ok(ApiResponse.success(employeService.findAll(), "Employés récupérés"));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<EmployeDTO>>> getAllEmployesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(employeService.findAll(pageable), "Employés paginés récupérés"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeById(@PathVariable Long id) {
        return employeService.findById(id)
                .map(emp -> ResponseEntity.ok(ApiResponse.success(emp, "Employé trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeByMatricule(@PathVariable String matricule) {
        return employeService.findByMatricule(matricule)
                .map(emp -> ResponseEntity.ok(ApiResponse.success(emp, "Employé trouvé")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departement/{departement}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByDepartement(@PathVariable String departement) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findByDepartement(departement), "Employés par département"));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findByStatut(statut), "Employés par statut"));
    }

    @GetMapping("/actifs")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesActifs() {
        return ResponseEntity.ok(ApiResponse.success(employeService.findActifs(), "Employés actifs"));
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findByManagerId(managerId), "Employés du manager"));
    }

    // ✅ Alias pour obtenir l'équipe d'un manager (utilisé par le front-end)
    @GetMapping("/manager/{managerId}/equipe")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEquipeByManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findByManagerId(managerId), "Équipe récupérée"));
    }


@DeleteMapping("/me/competences/{id}")
public ResponseEntity<?> deleteCompetence(
        @PathVariable Long id,
        Authentication auth
) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaim("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    EmployeCompetence ec = employeCompetenceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Compétence introuvable"));

    if (!ec.getEmploye().getId().equals(emp.getId())) {
        throw new RuntimeException("Unauthorized");
    }

    employeCompetenceRepository.delete(ec);

    return ResponseEntity.ok(Map.of("status", "deleted"));
}

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findByServiceId(serviceId), "Employés du service"));
    }

    @GetMapping("/solde-conges-faible/{seuil}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesSoldeCongesFaible(@PathVariable Integer seuil) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findSoldeCongesFaible(seuil), "Employés solde faible"));
    }

    // ===== CRUD =====
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeDTO>> createEmploye(@Valid @RequestBody EmployeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(employeService.create(dto), "Employé créé"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeDTO>> updateEmploye(@PathVariable Long id, @Valid @RequestBody EmployeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(employeService.update(id, dto), "Employé mis à jour"));
    }

    @PatchMapping("/{id}/profil")
    public ResponseEntity<ApiResponse<EmployeDTO>> mettreAJourProfil(
            @PathVariable Long id,
            @RequestParam(required = false) String poste,
            @RequestParam(required = false) Double salaire,
            @RequestParam(required = false) String departement) {
        return ResponseEntity.ok(ApiResponse.success(employeService.mettreAJourProfil(id, poste, salaire, departement), "Profil mis à jour"));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<EmployeDTO>> changerStatut(@PathVariable Long id, @RequestParam String statut) {
        return ResponseEntity.ok(ApiResponse.success(employeService.changerStatut(id, statut), "Statut changé"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmploye(@PathVariable Long id) {
        employeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Employé supprimé"));
    }

    // ===== STATISTIQUES =====
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countEmployes() {
        return ResponseEntity.ok(ApiResponse.success(employeService.count(), "Nombre d'employés"));
    }

    @GetMapping("/stats/departement")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByDepartement() {
        return ResponseEntity.ok(ApiResponse.success(employeService.countByDepartement(), "Stats par département"));
    }

    @GetMapping("/stats/statut")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        return ResponseEntity.ok(ApiResponse.success(employeService.countByStatut(), "Stats par statut"));
    }

    @GetMapping("/stats/masse-salariale")
    public ResponseEntity<ApiResponse<Double>> getMasseSalariale() {
        return ResponseEntity.ok(ApiResponse.success(employeService.calculerMasseSalariale(), "Masse salariale"));
    }

    @GetMapping("/stats/salaire-moyen")
    public ResponseEntity<ApiResponse<Double>> getSalaireMoyen() {
        return ResponseEntity.ok(ApiResponse.success(employeService.calculerSalaireMoyen(), "Salaire moyen"));
    }

    @GetMapping("/stats/tableau-bord")
    public ResponseEntity<ApiResponse<TableauBordEmployeDTO>> getStatsTableauBord() {
        return ResponseEntity.ok(ApiResponse.success(employeService.getStatsTableauBord(), "Tableau de bord"));
    }

    @GetMapping("/recents")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesRecents(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(employeService.findEmployesRecents(limit), "Employés récents"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> searchEmployes(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(employeService.search(keyword), "Résultats recherche"));
    }

    // ===== ESPACE EMPLOYÉ CONNECTÉ =====
    @GetMapping("/mon-profil")
    public ResponseEntity<ApiResponse<EmployeDTO>> getMonProfil(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        String email = userDetails.getUsername();
        EmployeDTO employe = employeService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));
        return ResponseEntity.ok(ApiResponse.success(employe, "Profil récupéré"));
    }



    @GetMapping("/mon-solde-conges")
    public ResponseEntity<ApiResponse<SoldeCongesDTO>> getMonSoldeConges(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        return ResponseEntity.ok(ApiResponse.success(employeService.getSoldeCongesByEmail(userDetails.getUsername()), "Solde récupéré"));
    }


    @GetMapping("/mes-competences")
    public ResponseEntity<ApiResponse<List<CompetenceEmployeDTO>>> getMesCompetences(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        return ResponseEntity.ok(ApiResponse.success(employeService.getCompetencesByEmail(userDetails.getUsername()), "Compétences récupérées"));
    }

    @GetMapping("/mes-formations")
    public ResponseEntity<ApiResponse<List<FormationEmployeDTO>>> getMesFormations(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        return ResponseEntity.ok(ApiResponse.success(employeService.getFormationsByEmail(userDetails.getUsername()), "Formations récupérées"));
    }

    @GetMapping("/mon-historique-conges")
    public ResponseEntity<ApiResponse<List<HistoriqueCongeDTO>>> getMonHistoriqueConges(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        return ResponseEntity.ok(ApiResponse.success(employeService.getHistoriqueCongesByEmail(userDetails.getUsername()), "Historique récupéré"));
    }

    @PatchMapping("/mon-profil")
    public ResponseEntity<ApiResponse<EmployeDTO>> updateMonProfil(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody UpdateProfilRequest request) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        return ResponseEntity.ok(ApiResponse.success(employeService.updateProfilByEmail(userDetails.getUsername(), request), "Profil mis à jour"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody ChangePasswordRequest request) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        employeService.changePasswordByEmail(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mot de passe changé"));
    }

    @PatchMapping("/change-email")
    public ResponseEntity<ApiResponse<EmployeDTO>> changeEmail(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String newEmail) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        return ResponseEntity.ok(ApiResponse.success(employeService.changeEmailByEmail(userDetails.getUsername(), newEmail), "Email changé"));
    }

    // ===== MÉTHODES POUR MANAGER ET ADMIN =====
    @GetMapping("/managers")
    @Operation(summary = "Liste tous les managers (admin)")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getAllManagers() {
        return ResponseEntity.ok(ApiResponse.success(employeService.findAllManagers(), "Managers récupérés"));
    }

    @GetMapping("/equipe")
    @Operation(summary = "Récupère l'équipe du manager connecté")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getMyEquipe(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.success(employeService.getEquipeByManagerEmail(email), "Équipe récupérée"));
    }

    @GetMapping("/manager/employe/{employeId}")
    @Operation(summary = "Détails d'un employé pour son manager")
    public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeForManager(@PathVariable Long employeId, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.success(employeService.getEmployeForManager(employeId, email), "Employé trouvé"));
    }

    @PutMapping("/{employeId}/manager/{managerId}")
    @Operation(summary = "Assigne un manager à un employé (admin)")
    public ResponseEntity<ApiResponse<EmployeDTO>> assignManager(@PathVariable Long employeId, @PathVariable(required = false) Long managerId) {
        return ResponseEntity.ok(ApiResponse.success(employeService.updateManager(employeId, managerId), "Manager assigné"));
    }


   // 🔥 GET compétences employé
// ================================
// 🔥 GET compétences employé connecté
// ================================
@GetMapping("/me/competences")
public List<Map<String, Object>> getMyCompetences(Authentication auth) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaim("email");

    // 🔥 AJOUT IMPORTANT
    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    // 🔥 maintenant emp existe
    List<EmployeCompetence> list =
            employeCompetenceRepository.findByEmploye_Id(emp.getId());

    List<Map<String, Object>> result = new ArrayList<>();

    for (EmployeCompetence ec : list) {

        Map<String, Object> item = new HashMap<>();

        item.put("id", ec.getId());
        item.put("nom", ec.getCompetence().getNom());
        item.put("competenceId", ec.getCompetence().getId());
        item.put("niveau", convertLevelToInt(ec.getNiveau()));

        result.add(item);
    }

    return result;
}

@PostMapping("/me/competences")
public ResponseEntity<?> addCompetence(
        @RequestBody Map<String, Object> body,
        Authentication auth
) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaim("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    Long compId = Long.valueOf(body.get("competenceId").toString());
    int niveau = Integer.parseInt(body.get("niveau").toString());

    employeService.addCompetence(emp.getId(), compId, niveau);

    return ResponseEntity.ok(Map.of("status", "added"));
}


@PutMapping("/me/competences")
public ResponseEntity<?> updateCompetences(
        @RequestBody List<Map<String, Object>> body,
        Authentication auth
) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaim("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    for (Map<String, Object> item : body) {

        Long compId = Long.valueOf(item.get("competenceId").toString());
        int niveau = Integer.parseInt(item.get("niveau").toString());

        EmployeCompetence ec = employeCompetenceRepository
                .findByEmploye_IdAndCompetence_Id(emp.getId(), compId)
                .orElseThrow(() -> new RuntimeException("Compétence non trouvée"));

        ec.setNiveau(convertToLevel(niveau));

        employeCompetenceRepository.save(ec);
    }

    return ResponseEntity.ok(Map.of("status", "updated"));
}

private String convertToLevel(int niveau) {
    return switch (niveau) {
        case 1 -> "DEBUTANT";
        case 2 -> "INTERMEDIAIRE";
        case 3 -> "AVANCE";
        case 4 -> "EXPERT";
        default -> "DEBUTANT";
    };
}
private int convertLevelToInt(String niveau) {
    return switch (niveau) {
        case "DEBUTANT" -> 1;
        case "INTERMEDIAIRE" -> 2;
        case "AVANCE" -> 3;
        case "EXPERT" -> 4;
        default -> 1;
    };
}






}