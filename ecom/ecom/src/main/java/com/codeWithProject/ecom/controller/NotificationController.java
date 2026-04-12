package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Notification;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeRepository employeRepository;

    /**
     * Récupère toutes les notifications de l'employé connecté
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        Long employeId = extractEmployeId(jwt);
        List<Notification> notifications = notificationService.getNotificationsByEmployeId(employeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", notifications);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère uniquement les notifications non lues
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        Long employeId = extractEmployeId(jwt);
        List<Notification> notifications = notificationService.getUnreadByEmploye(employeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", notifications);
        return ResponseEntity.ok(response);
    }

    /**
     * Marque une notification comme lue
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification marquée comme lue");
        return ResponseEntity.ok(response);
    }

    /**
     * Marque toutes les notifications de l'employé comme lues
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        Long employeId = extractEmployeId(jwt);
        notificationService.markAllAsRead(employeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Crée une notification pour l'employé connecté
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> payload) {
        Long employeId = extractEmployeId(jwt);
        String message = (String) payload.get("message");
        String type = (String) payload.get("type");
        Long relatedDemandeId = payload.get("relatedDemandeId") != null ?
                Long.valueOf(payload.get("relatedDemandeId").toString()) : null;

        notificationService.createNotification(employeId, message, type, relatedDemandeId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification créée");
        return ResponseEntity.ok(response);
    }

    /**
     * Extrait l'ID de l'employé à partir du JWT
     */
    private Long extractEmployeId(Jwt jwt) {
        // Méthode 1: Récupérer depuis la claim "employeId"
        Long employeId = jwt.getClaim("employeId");
        if (employeId != null && employeId > 0) {
            log.debug("EmployeId trouvé dans JWT: {}", employeId);
            return employeId;
        }

        // Méthode 2: Récupérer depuis l'email
        String email = jwt.getClaimAsString("email");
        if (email != null) {
            Optional<Employe> employeOpt = employeRepository.findByEmail(email);
            if (employeOpt.isPresent()) {
                Employe employe = employeOpt.get();
                log.debug("Employe trouvé par email: {} -> ID: {}", email, employe.getId());
                return employe.getId();
            }
        }

        // Méthode 3: Fallback - à corriger
        log.warn("EmployeId non trouvé dans JWT, fallback à 1 (à corriger)");
        return 1L;
    }
}