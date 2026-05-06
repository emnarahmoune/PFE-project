package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.KeycloakAdminService;
import com.codeWithProject.ecom.service.ManagerProfileService;
import com.codeWithProject.ecom.service.ManagerService;
import com.codeWithProject.ecom.service.PhotoService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import com.codeWithProject.ecom.service.dto.ManagerProfileDTO;
import com.codeWithProject.ecom.service.dto.PhotoUploadResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final WorkflowService workflowService;
    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final ManagerProfileService managerProfileService;
    private final KeycloakAdminService keycloakAdminService;
    private final PhotoService photoService;
    private final ManagerService managerService;

    // ================= PROFILE =================

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ManagerProfileDTO>> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);

        log.info("GET PROFILE - {}", email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        managerProfileService.getProfile(jwt),
                        "Profil récupéré"
                )
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ManagerProfileDTO>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ManagerProfileDTO dto
    ) {
        String email = extractEmail(jwt);

        log.info("UPDATE PROFILE - {}", email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        managerProfileService.updateProfile(jwt, dto),
                        "Profil mis à jour"
                )
        );
    }

    // ================= CALENDRIER =================

    @GetMapping("/calendar-events")
    public ResponseEntity<ApiResponse<List<CalendarEventDTO>>> getCalendarEvents(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = extractEmail(jwt);

        List<CalendarEventDTO> events = managerService.getCalendarEvents(email);

        return ResponseEntity.ok(
                ApiResponse.success(events, "Événements calendrier récupérés avec succès")
        );
    }

    // ================= PASSWORD =================

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest dto
    ) {
        String email = extractEmail(jwt);

        log.info("CHANGE PASSWORD MANAGER - {}", email);

        try {
            managerProfileService.changePassword(jwt, dto);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Mot de passe modifié")
            );

        } catch (Exception e) {
            log.error("Erreur changement mot de passe manager", e);

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ================= PHOTO =================

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        String email = extractEmail(jwt);

        log.info("UPLOAD PHOTO - {}", email);

        ManagerProfileDTO profile = managerProfileService.getProfile(jwt);

        String photoUrl = photoService.uploadPhoto(profile.getId(), file);

        return ResponseEntity.ok(
                ApiResponse.success(
                        PhotoUploadResponse.builder()
                                .photoUrl(photoUrl)
                                .message("Photo uploadée")
                                .build(),
                        "Photo uploadée"
                )
        );
    }

    @DeleteMapping("/photo")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@AuthenticationPrincipal Jwt jwt) {
        ManagerProfileDTO profile = managerProfileService.getProfile(jwt);

        photoService.deletePhoto(profile.getId());

        return ResponseEntity.ok(
                ApiResponse.success(null, "Photo supprimée")
        );
    }

    // ================= STATS =================

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);

        Employe manager = employeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Manager introuvable"));

        List<Employe> equipe = employeRepository.findByManagerId(manager.getId());

        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);
        int congesEnAttente = tasks != null ? tasks.size() : 0;

        long totalEmployes = equipe.size();

        long employesActifs = equipe.stream()
                .filter(e -> Boolean.TRUE.equals(e.getActif()))
                .count();

        double tauxPresence = totalEmployes == 0
                ? 100.0
                : (employesActifs * 100.0) / totalEmployes;

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalEmployes", totalEmployes);
        stats.put("employesActifs", employesActifs);
        stats.put("congesEnAttente", congesEnAttente);
        stats.put("tauxPresence", Math.round(tauxPresence));

        stats.put("nbEmployes", totalEmployes);
        stats.put("demandesEnAttente", congesEnAttente);
        stats.put("demandesApprouvees", demandeCongeRepository.countByStatut("APPROUVE"));
        stats.put("demandesRefusees", demandeCongeRepository.countByStatut("REFUSE"));

        log.info("STATS MANAGER {} => total={}, actifs={}, attente={}",
                email, totalEmployes, employesActifs, congesEnAttente);

        return ResponseEntity.ok(
                ApiResponse.success(stats, "Statistiques manager récupérées")
        );
    }

    // ================= CONGES =================

    @GetMapping("/conges")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConges(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = extractEmail(jwt);

        log.info("GET /api/manager/conges - manager : {}", email);

        List<Map<String, Object>> tasks = workflowService.getManagerTasks(email);

        enrichTasksWithEmployeePhotos(tasks);

        log.info("NB TASKS MANAGER = {}", tasks != null ? tasks.size() : 0);

        return ResponseEntity.ok(
                ApiResponse.success(tasks, "Congés manager récupérés")
        );
    }

    // ================= HELPERS PHOTO =================

    private void enrichTasksWithEmployeePhotos(List<Map<String, Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (Map<String, Object> task : tasks) {
            if (task == null) {
                continue;
            }

            Employe employe = findEmployeFromTask(task);

            if (employe == null) {
                task.putIfAbsent("photoUrl", null);
                task.putIfAbsent("employePhotoProfil", null);
                task.putIfAbsent("employePhotoUrl", null);
                continue;
            }

            String photo = employe.getPhotoUrl();

            task.put("photoUrl", photo);
            task.put("employePhotoProfil", photo);
            task.put("employePhotoUrl", photo);

            task.putIfAbsent("employeId", employe.getId());
            task.putIfAbsent("employeNom", employe.getNom());
            task.putIfAbsent("employePrenom", employe.getPrenom());
            task.putIfAbsent("employeEmail", employe.getEmail());
        }
    }

    private Employe findEmployeFromTask(Map<String, Object> task) {
        Long employeId = extractLong(task.get("employeId"));

        if (employeId == null) {
            employeId = extractLong(task.get("employeeId"));
        }

        if (employeId == null) {
            employeId = extractLong(task.get("idEmploye"));
        }

        if (employeId != null) {
            Optional<Employe> byId = employeRepository.findById(employeId);

            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String email = extractString(task.get("employeEmail"));

        if (email == null || email.isBlank()) {
            email = extractString(task.get("employeeEmail"));
        }

        if (email == null || email.isBlank()) {
            email = extractString(task.get("email"));
        }

        if (email != null && !email.isBlank()) {
            return employeRepository.findByEmailIgnoreCase(email.trim())
                    .orElse(null);
        }

        return null;
    }

    private Long extractLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractString(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            email = jwt.getSubject();
        }

        return email != null ? email.trim().toLowerCase() : null;
    }
}