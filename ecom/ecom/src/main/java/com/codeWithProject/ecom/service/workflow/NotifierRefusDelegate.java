package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.entity.DemandeConge;
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
public class NotifierRefusDelegate implements JavaDelegate {

    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== NOTIFICATION REFUS ===");

        // Récupérer l'ID de la demande depuis les variables du workflow
        Long demandeId = execution.getVariable("demandeId") != null ?
                Long.valueOf(execution.getVariable("demandeId").toString()) : null;

        if (demandeId == null) {
            log.warn("Aucune variable 'demandeId' trouvée dans le workflow");
            return;
        }

        DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
        if (demande == null) {
            log.warn("Demande non trouvée pour demandeId: {}", demandeId);
            return;
        }

        // Éviter la double notification si déjà refusée (par VerifierSoldeDelegate)
        if ("REFUSE".equals(demande.getStatut())) {
            log.info("Demande {} déjà refusée, notification déjà envoyée", demande.getId());
            return;
        }

        // Récupérer le motif : priorité à motifRefus, puis commentaireManager, puis commentaireRH
        String motif = (String) execution.getVariable("motifRefus");
        if (motif == null || motif.trim().isEmpty()) {
            motif = (String) execution.getVariable("commentaireManager");
        }
        if (motif == null || motif.trim().isEmpty()) {
            motif = (String) execution.getVariable("commentaireRH");
        }
        if (motif == null || motif.trim().isEmpty()) {
            motif = "Refus sans motif";
        }

        // Mettre à jour la demande
        demande.refuserAvecMotif(motif);
        demandeRepository.save(demande);

        // Créer une notification
        String message = String.format("❌ Votre demande du %s au %s a été refusée. Motif : %s",
                demande.getDateDebut(), demande.getDateFin(), motif);
        notificationService.createNotification(demande.getEmploye().getId(), message, "ERROR", demande.getId());

        log.info("✅ Demande {} refusée avec motif: {}", demande.getId(), motif);
    }
}