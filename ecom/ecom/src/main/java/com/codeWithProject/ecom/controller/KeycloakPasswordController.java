package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.KeycloakAdminService;
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

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profil Administrateur RH", description = "Changement mot de passe via Keycloak")
@PreAuthorize("hasRole('ADMIN_RH')")
public class KeycloakPasswordController {

    private final KeycloakAdminService keycloakAdminService;

    @PostMapping("/change-password")
    @Operation(summary = "Change le mot de passe de l'administrateur RH via Keycloak")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordDTO dto) {

        String email = extractEmail(jwt);
        log.info("POST /api/admin/profile/change-password - admin : {}", email);

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le nouveau mot de passe et sa confirmation ne correspondent pas"));
        }

        String userId = jwt.getSubject();

        try {
            keycloakAdminService.resetPassword(userId, dto.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success(null, "Mot de passe modifié avec succès dans Keycloak"));
        } catch (Exception e) {
            log.error("Erreur changement mot de passe Keycloak", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getSubject();
        return email;
    }
}