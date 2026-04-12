package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Notification;
import com.codeWithProject.ecom.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(Long employeId, String message, String type, Long relatedDemandeId) {
        Notification notif = new Notification();
        notif.setEmployeId(employeId);
        notif.setMessage(message);
        notif.setType(type);
        notif.setLu(false);
        notif.setDateCreation(LocalDateTime.now());
        notif.setRelatedDemandeId(relatedDemandeId);
        notificationRepository.save(notif);
        log.info("✅ Notification créée pour employé {}: {}", employeId, message);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadByEmploye(Long employeId) {
        return notificationRepository.findByEmployeIdAndLuFalseOrderByDateCreationDesc(employeId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByEmployeId(Long employeId) {
        return notificationRepository.findByEmployeIdOrderByDateCreationDesc(employeId);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }

    @Transactional
    public void markAllAsRead(Long employeId) {
        notificationRepository.markAllAsReadByEmploye(employeId);
    }

    // Méthodes utilitaires supplémentaires
    @Transactional
    public void createRefusNotification(Long employeId, String message, Long demandeId) {
        createNotification(employeId, message, "ERROR", demandeId);
    }

    @Transactional
    public void createApprobationNotification(Long employeId, String message, Long demandeId) {
        createNotification(employeId, message, "SUCCESS", demandeId);
    }
}