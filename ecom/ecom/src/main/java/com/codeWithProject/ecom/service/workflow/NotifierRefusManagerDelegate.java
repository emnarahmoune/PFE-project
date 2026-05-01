package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotifierRefusManagerDelegate implements JavaDelegate {

    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== NOTIFICATION REFUS MANAGER ===");

        Long demandeId = (Long) execution.getVariable("demandeId");
        if (demandeId == null) return;

        var demande = demandeRepository.findById(demandeId).orElse(null);
        if (demande == null) return;

        // Le statut a déjà été mis à jour par WorkflowService.processManagerDecision()
        // On se contente d'envoyer la notification
        String motif = (String) execution.getVariable("commentaireManager");
        if (motif == null || motif.isBlank()) motif = "Refusé par le manager";

        // Éviter d'envoyer une notification si déjà fait
        if ("REFUSE_MANAGER".equals(demande.getStatut())) {
            String message = String.format("❌ Votre demande du %s au %s a été refusée par votre manager. Motif : %s",
                    demande.getDateDebut(), demande.getDateFin(), motif);
            notificationService.createNotification(demande.getEmploye().getId(), message, "ERROR", demandeId);
            log.info("✅ Notification de refus manager envoyée pour demande {}", demandeId);
        } else {
            log.warn("Demande {} non en état REFUSE_MANAGER ({}) – notification ignorée", demandeId, demande.getStatut());
        }
    }
}