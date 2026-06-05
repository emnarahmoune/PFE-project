package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeCompetence;
import com.codeWithProject.ecom.repository.EmployeCompetenceRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.EmployeService;
import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import com.codeWithProject.ecom.service.dto.CompetenceEmployeDTO;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import com.codeWithProject.ecom.service.dto.FormationEmployeDTO;
import com.codeWithProject.ecom.service.dto.HistoriqueCongeDTO;
import com.codeWithProject.ecom.service.dto.SoldeCongesDTO;
import com.codeWithProject.ecom.service.dto.TableauBordEmployeDTO;
import com.codeWithProject.ecom.service.dto.UpdateProfilRequest;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.EmployeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employés", description = "API de gestion des employés")
public class EmployeController {

    private final EmployeService employeService;
    private final EmployeRepository employeRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final EmployeMapper employeMapper;

    // ===== DTO INTERNE POUR ASSIGNER UN MANAGER =====

    @Getter
    @Setter
    public static class AssignManagerRequest {
        private Long managerId;
    }

    // ===== HELPERS AUTH =====

    private String extractEmailFromAuthentication(Authentication authentication) {
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

            return email;
        }

        return authentication.getName();
    }

    // ===== LISTES GÉNÉRALES =====

   @GetMapping
public ResponseEntity<ApiResponse<Page<EmployeDTO>>> getAllEmployes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

    return ResponseEntity.ok(
            ApiResponse.success(employeService.findAll(pageable), "Employés récupérés")
    );
}

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<EmployeDTO>>> getAllEmployesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                ApiResponse.success(employeService.findAll(pageable), "Employés paginés récupérés")
        );
    }

  @GetMapping("/{id}")
public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeById(@PathVariable Long id) {
    EmployeDTO dto = employeService.findById(id)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    dto.setFormations(employeService.getFormationsByEmployeId(id));
    dto.setConges(employeService.getCongesByEmployeId(id));
    dto.setEvaluations(employeService.getEvaluationsByEmployeId(id));

    return ResponseEntity.ok(ApiResponse.success(dto, "OK"));
}

 @GetMapping("/matricule/{matricule}")
public ResponseEntity<?> getByMatricule(@PathVariable String matricule) {
    return employeRepository.findByMatricule(matricule)
            .map(employe -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", employe
            )))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Employé introuvable"
            )));
}

    @GetMapping("/departement/{departement}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByDepartement(
            @PathVariable String departement
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findByDepartement(departement), "Employés par département")
        );
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByStatut(
            @PathVariable String statut
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findByStatut(statut), "Employés par statut")
        );
    }

    @GetMapping("/actifs")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesActifs() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findActifs(), "Employés actifs")
        );
    }

    @GetMapping("/me")
    public EmployeDTO getCurrentUser(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return employeMapper.toDto(employe);
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByManager(
            @PathVariable Long managerId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findByManagerId(managerId), "Employés du manager")
        );
    }

    @GetMapping("/manager/{managerId}/equipe")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEquipeByManager(
            @PathVariable Long managerId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findByManagerId(managerId), "Équipe récupérée")
        );
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesByService(
            @PathVariable Long serviceId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findByServiceId(serviceId), "Employés du service")
        );
    }

    @GetMapping("/solde-conges-faible/{seuil}")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesSoldeCongesFaible(
            @PathVariable Integer seuil
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findSoldeCongesFaible(seuil), "Employés solde faible")
        );
    }

    // ===== CRUD =====

   @PostMapping
public ResponseEntity<ApiResponse<EmployeDTO>> createEmploye(
        @Valid @RequestBody EmployeDTO dto
) {

    if (dto.getRole() == null) {
        dto.setRole("USER"); // 🔥 sécurité
    }

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(employeService.create(dto), "Employé créé"));
}

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeDTO>> updateEmploye(
            @PathVariable Long id,
            @Valid @RequestBody EmployeDTO dto
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.update(id, dto), "Employé mis à jour")
        );
    }

    @PatchMapping("/{id}/profil")
    public ResponseEntity<ApiResponse<EmployeDTO>> mettreAJourProfil(
            @PathVariable Long id,
            @RequestParam(required = false) String poste,
            @RequestParam(required = false) Double salaire,
            @RequestParam(required = false) String departement
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        employeService.mettreAJourProfil(id, poste, salaire, departement),
                        "Profil mis à jour"
                )
        );
    }

    @PutMapping("/statut/{id}")
    public ResponseEntity<?> changeStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String statut = body.get("statut");
        return ResponseEntity.ok(employeService.changerStatut(id, statut));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmploye(@PathVariable Long id) {
        employeService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Employé désactivé")
        );
    }

    // ===== ASSIGNATION MANAGER =====

    /*
     * Route compatible avec ton frontend actuel :
     * PUT /api/employes/manager/7
     * Body :
     * {
     *   "managerId": 2
     * }
     */
    @PutMapping("/manager/{employeId}")
    @Operation(summary = "Assigne un manager à un employé")
    public ResponseEntity<ApiResponse<EmployeDTO>> assignerManagerAvecBody(
            @PathVariable Long employeId,
            @RequestBody AssignManagerRequest request
    ) {
        if (request == null || request.getManagerId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "managerId est obligatoire"));
        }

        EmployeDTO updated = employeService.updateManager(employeId, request.getManagerId());

        return ResponseEntity.ok(
                ApiResponse.success(updated, "Manager assigné")
        );
    }


    @PostMapping("/mon-profil/photo")
public ResponseEntity<ApiResponse<Map<String, String>>> uploadMaPhoto(
        Authentication authentication,
        @RequestParam("file") MultipartFile file
) throws IOException {
    String email = extractEmailFromAuthentication(authentication);

    if (email == null || email.isBlank()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
    }

    String photoUrl = employeService.uploadPhotoProfilByEmail(email, file);

    Map<String, String> data = new HashMap<>();
    data.put("photoUrl", photoUrl);

    return ResponseEntity.ok(
            ApiResponse.success(data, "Photo mise à jour")
    );
}

@DeleteMapping("/mon-profil/photo")
public ResponseEntity<ApiResponse<Void>> deleteMaPhoto(Authentication authentication) {
    String email = extractEmailFromAuthentication(authentication);

    if (email == null || email.isBlank()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
    }

    employeService.deletePhotoProfilByEmail(email);

    return ResponseEntity.ok(
            ApiResponse.success(null, "Photo supprimée")
    );
}

    /*
     * Ancienne route conservée :
     * PUT /api/employes/7/manager/2
     */
    @PutMapping("/{employeId}/manager/{managerId}")
    @Operation(summary = "Assigne un manager à un employé avec managerId dans l'URL")
    public ResponseEntity<ApiResponse<EmployeDTO>> assignManagerAvecPath(
            @PathVariable Long employeId,
            @PathVariable Long managerId
    ) {
        EmployeDTO updated = employeService.updateManager(employeId, managerId);

        return ResponseEntity.ok(
                ApiResponse.success(updated, "Manager assigné")
        );
    }

    /*
     * Option pour retirer le manager :
     * DELETE /api/employes/7/manager
     */
    @DeleteMapping("/{employeId}/manager")
    @Operation(summary = "Retire le manager d'un employé")
    public ResponseEntity<ApiResponse<EmployeDTO>> retirerManager(
            @PathVariable Long employeId
    ) {
        EmployeDTO updated = employeService.updateManager(employeId, null);

        return ResponseEntity.ok(
                ApiResponse.success(updated, "Manager retiré")
        );
    }

    // ===== STATISTIQUES =====

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countEmployes() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.count(), "Nombre d'employés")
        );
    }

    @GetMapping("/stats/departement")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByDepartement() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.countByDepartement(), "Stats par département")
        );
    }

    @GetMapping("/stats/statut")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsByStatut() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.countByStatut(), "Stats par statut")
        );
    }

    @GetMapping("/stats/masse-salariale")
    public ResponseEntity<ApiResponse<Double>> getMasseSalariale() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.calculerMasseSalariale(), "Masse salariale")
        );
    }

    @GetMapping("/stats/salaire-moyen")
    public ResponseEntity<ApiResponse<Double>> getSalaireMoyen() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.calculerSalaireMoyen(), "Salaire moyen")
        );
    }

    @GetMapping("/stats/tableau-bord")
    public ResponseEntity<ApiResponse<TableauBordEmployeDTO>> getStatsTableauBord() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.getStatsTableauBord(), "Tableau de bord")
        );
    }

    @GetMapping("/recents")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getEmployesRecents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findEmployesRecents(limit), "Employés récents")
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> searchEmployes(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.search(keyword), "Résultats recherche")
        );
    }

    // ===== ESPACE EMPLOYÉ CONNECTÉ =====

    @GetMapping("/mon-profil")
public ResponseEntity<ApiResponse<EmployeDTO>> getMonProfil(Authentication authentication){
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        EmployeDTO employe = employeService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return ResponseEntity.ok(
                ApiResponse.success(employe, "Profil récupéré")
        );
    }

    @GetMapping("/mon-solde-conges")
    public ResponseEntity<ApiResponse<SoldeCongesDTO>> getMonSoldeConges(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.getSoldeCongesByEmail(email), "Solde récupéré")
        );
    }

    @GetMapping("/mes-competences")
    public ResponseEntity<ApiResponse<List<CompetenceEmployeDTO>>> getMesCompetences(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.getCompetencesByEmail(email), "Compétences récupérées")
        );
    }

    @GetMapping("/mes-formations")
    public ResponseEntity<ApiResponse<List<FormationEmployeDTO>>> getMesFormations(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.getFormationsByEmail(email), "Formations récupérées")
        );
    }

    @GetMapping("/mon-historique-conges")
    public ResponseEntity<ApiResponse<List<HistoriqueCongeDTO>>> getMonHistoriqueConges(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.getHistoriqueCongesByEmail(email), "Historique récupéré")
        );
    }

@PutMapping("/mon-profil")
public ResponseEntity<ApiResponse<EmployeDTO>> updateMonProfilPut(
        Authentication authentication,
        @Valid @RequestBody UpdateProfilRequest request
) {
    String email = extractEmailFromAuthentication(authentication);

    if (email == null || email.isBlank()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
    }

    log.info("Mise à jour du profil connecté: {}", email);

    EmployeDTO updated = employeService.updateProfilByEmail(email, request);

    return ResponseEntity.ok(
            ApiResponse.success(updated, "Profil mis à jour")
    );
}


   @PostMapping("/change-password")
public ResponseEntity<ApiResponse<Void>> changePassword(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ChangePasswordRequest dto
) {
    employeService.changePassword(jwt, dto);

    return ResponseEntity.ok(
            ApiResponse.success(null, "Mot de passe modifié")
    );
}

    @PatchMapping("/change-email")
    public ResponseEntity<ApiResponse<EmployeDTO>> changeEmail(
            Authentication authentication,
            @RequestParam String newEmail
    ) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.changeEmailByEmail(email, newEmail), "Email changé")
        );
    }

    // ===== MÉTHODES POUR MANAGER ET ADMIN =====

    @GetMapping("/managers")
    @Operation(summary = "Liste tous les managers")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getAllManagers() {
        return ResponseEntity.ok(
                ApiResponse.success(employeService.findAllManagers(), "Managers récupérés")
        );
    }

    @GetMapping("/equipe")
    @Operation(summary = "Récupère l'équipe du manager connecté")
    public ResponseEntity<ApiResponse<List<EmployeDTO>>> getMyEquipe(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.getEquipeByManagerEmail(email), "Équipe récupérée")
        );
    }

    @GetMapping("/manager/employe/{employeId}")
    @Operation(summary = "Détails d'un employé pour son manager")
    public ResponseEntity<ApiResponse<EmployeDTO>> getEmployeForManager(
            @PathVariable Long employeId,
            Authentication authentication
    ) {
        String email = extractEmailFromAuthentication(authentication);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        }

        return ResponseEntity.ok(
                ApiResponse.success(employeService.getEmployeForManager(employeId, email), "Employé trouvé")
        );
    }

    // ===== COMPÉTENCES EMPLOYÉ CONNECTÉ =====

    @GetMapping("/me/competences")
    public List<Map<String, Object>> getMyCompetences(Authentication authentication) {
        String email = extractEmailFromAuthentication(authentication);

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

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
            Authentication authentication
    ) {
        String email = extractEmailFromAuthentication(authentication);

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
            Authentication authentication
    ) {
        String email = extractEmailFromAuthentication(authentication);

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

    @DeleteMapping("/me/competences/{id}")
    public ResponseEntity<?> deleteCompetence(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = extractEmailFromAuthentication(authentication);

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
        if (niveau == null) {
            return 1;
        }

        return switch (niveau) {
            case "DEBUTANT" -> 1;
            case "INTERMEDIAIRE" -> 2;
            case "AVANCE" -> 3;
            case "EXPERT" -> 4;
            default -> 1;
        };
    }




}