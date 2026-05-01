package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.AdminProfileService;
import com.codeWithProject.ecom.service.dto.AdminProfileDTO;
import com.codeWithProject.ecom.service.dto.AdminProfileUpdateDTO;
import com.codeWithProject.ecom.service.dto.ChangePasswordDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profil Administrateur RH", description = "Gestion du profil de l'administrateur RH connecté")
@PreAuthorize("hasRole('ADMIN_RH')")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    @Operation(summary = "Récupère le profil de l'administrateur RH authentifié")
    public ResponseEntity<ApiResponse<AdminProfileDTO>> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("GET /api/admin/profile - admin : {}", email);

        AdminProfileDTO profile = adminProfileService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profil récupéré avec succès"));
    }

    @PutMapping
    @Operation(summary = "Met à jour les informations du profil (hors mot de passe)")
    public ResponseEntity<ApiResponse<AdminProfileDTO>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminProfileUpdateDTO dto) {
        String email = extractEmail(jwt);
        log.info("PUT /api/admin/profile - admin : {}", email);
        AdminProfileDTO updated = adminProfileService.updateProfile(email, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profil mis à jour avec succès"));
    }
    // ========== PHOTO ==========

    @PostMapping("/photo")
    @Operation(summary = "Téléverse une photo de profil pour l'admin")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        String email = extractEmail(jwt);
        log.info("POST /api/admin/profile/photo - admin : {}", email);
        String photoUrl = adminProfileService.uploadPhoto(email, file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("photoUrl", photoUrl), "Photo mise à jour"));
    }

    @DeleteMapping("/photo")
    @Operation(summary = "Supprime la photo de profil de l'admin")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        log.info("DELETE /api/admin/profile/photo - admin : {}", email);
        adminProfileService.deletePhoto(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo supprimée"));
    }
    // ========== MÉTHODES PRIVÉES ==========

    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
}