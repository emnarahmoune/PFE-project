package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.KeycloakAdminService;
import com.codeWithProject.ecom.service.ManagerProfileService;
import com.codeWithProject.ecom.service.PhotoService;
import com.codeWithProject.ecom.service.dto.ChangePasswordDTO;
import com.codeWithProject.ecom.service.dto.ManagerProfileDTO;
import com.codeWithProject.ecom.service.dto.PhotoUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/manager/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profil Manager", description = "Gestion du profil du manager connecté")
@PreAuthorize("hasRole('manager')")
public class ManagerProfileController {

    private final ManagerProfileService managerProfileService;
    private final KeycloakAdminService keycloakAdminService;
    private final PhotoService photoService;
    @GetMapping
    @Operation(summary = "Récupère le profil du manager authentifié")
    public ResponseEntity<ApiResponse<ManagerProfileDTO>> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("GET /api/manager/profile - manager : {}", email);
        ManagerProfileDTO profile = managerProfileService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profil récupéré avec succès"));
    }

    @PutMapping
    @Operation(summary = "Met à jour les informations du profil (hors mot de passe)")
    public ResponseEntity<ApiResponse<ManagerProfileDTO>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ManagerProfileDTO dto) {
        String email = extractEmail(jwt);
        log.info("PUT /api/manager/profile - manager : {}", email);
        ManagerProfileDTO updated = managerProfileService.updateProfile(email, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profil mis à jour avec succès"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change le mot de passe du manager (via Keycloak)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordDTO dto) {
        String email = extractEmail(jwt);
        log.info("POST /api/manager/profile/change-password - manager : {}", email);

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le nouveau mot de passe et sa confirmation ne correspondent pas"));
        }

        // Récupérer l'identifiant Keycloak (subject)
        String userId = jwt.getSubject();
        try {
            keycloakAdminService.resetPassword(userId, dto.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "Mot de passe modifié avec succès"));
        } catch (Exception e) {
            log.error("Erreur changement mot de passe Keycloak", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== MÉTHODES PRIVÉES ==========
    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Télécharge une photo de profil pour le manager")
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {

        String email = extractEmail(jwt);
        log.info("Upload photo pour manager : {}", email);

        // Récupérer l'ID du manager
        ManagerProfileDTO profile = managerProfileService.getProfile(email);
        String photoUrl = photoService.uploadPhoto(profile.getId(), file);

        PhotoUploadResponse response = PhotoUploadResponse.builder()
                .photoUrl(photoUrl)
                .message("Photo téléchargée avec succès")
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @DeleteMapping("/photo")
    @Operation(summary = "Supprime la photo de profil du manager")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        ManagerProfileDTO profile = managerProfileService.getProfile(email);
        photoService.deletePhoto(profile.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Photo supprimée"));
    }
}