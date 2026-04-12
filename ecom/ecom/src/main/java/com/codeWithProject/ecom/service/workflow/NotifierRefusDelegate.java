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
    public void execute(DelegateExecution execution) throws Exception {
        log.info("=== NOTIFICATION REFUS ===");

        // Récupérer la demande via processInstanceId
        String processInstanceId = execution.getProcessInstanceId();
        // ✅ Correction: findByProcessInstanceId retourne Optional
        DemandeConge demande = demandeRepository.findByProcessInstanceId(processInstanceId)
                .orElse(null);

        if (demande == null) {
            log.warn("Demande non trouvée pour processInstanceId: {}", processInstanceId);
            return;
        }

        // Vérifier si déjà refusée (par VerifierSoldeDelegate)
        if ("REFUSE".equals(demande.getStatut())) {
            log.info("Demande {} déjà refusée, notification déjà envoyée par VerifierSoldeDelegate", demande.getId());
            return;
        }

        // Récupérer le motif de refus (priorité au manager puis RH)
        String motif = (String) execution.getVariable("commentaireManager");
        if (motif == null || motif.trim().isEmpty()) {
            motif = (String) execution.getVariable("commentaireRH");
        }
        if (motif == null || motif.trim().isEmpty()) {
            motif = "Refus sans motif";
        }

        // Mettre à jour la demande en base
        demande.refuserAvecMotif(motif);
        demandeRepository.save(demande);

        // Créer une notification
        String message = String.format("❌ Votre demande du %s au %s a été refusée. Motif : %s",
                demande.getDateDebut(), demande.getDateFin(), motif);
        notificationService.createNotification(
                demande.getEmploye().getId(), message, "ERROR", demande.getId());

        log.info("✅ Demande {} refusée avec motif: {}", demande.getId(), motif);
    }
}