package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.NotificationService;
import com.codeWithProject.ecom.service.SoldeCongeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerifierSoldeDelegate implements JavaDelegate {

    private final SoldeCongeService soldeCongeService;
    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;
    private final RuntimeService runtimeService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== Vérification du solde ===");

        String processInstanceId = execution.getProcessInstanceId();
        Long employeId = Long.valueOf(execution.getVariable("employeId").toString());
        Integer nbJours = ((Number) execution.getVariable("nbJours")).intValue();
        Long demandeId = execution.getVariable("demandeId") != null ?
                Long.valueOf(execution.getVariable("demandeId").toString()) : null;
        Boolean urgente = (Boolean) execution.getVariable("urgente");
        if (urgente == null) urgente = false;

        int annee = LocalDate.now().getYear();

        // Utilisation du service centralisé
        int soldeRestant = soldeCongeService.getSoldeRestant(employeId, annee);
        boolean soldeSuffisant = soldeCongeService.verifierSoldeSuffisant(employeId, nbJours, annee);

        execution.setVariable("soldeSuffisant", soldeSuffisant);
        execution.setVariable("soldeActuel", soldeRestant);
        execution.setVariable("nbJours", nbJours);

        log.info("Employé ID: {}, Solde restant: {}, Demandé: {}, Suffisant: {}, Urgente: {}",
                employeId, soldeRestant, nbJours, soldeSuffisant, urgente);

        // Refus automatique si solde insuffisant et non urgent
        if (!soldeSuffisant && !urgente) {
            log.info("❌ Demande {} - Refus automatique (solde insuffisant)", demandeId);
            String motif = String.format("Solde de congés insuffisant (solde restant: %d jours, demandé: %d jours)",
                    soldeRestant, nbJours);

            refuserDemandeEtNotifier(demandeId, motif, employeId);

            // Supprimer l'instance du workflow
            try {
                runtimeService.deleteProcessInstance(processInstanceId,
                        "Refus automatique pour solde insuffisant", true, true);
                log.info("Instance de processus {} supprimée", processInstanceId);
            } catch (Exception e) {
                log.error("Erreur lors de la suppression: {}", e.getMessage());
            }
        } else if (!soldeSuffisant && urgente) {
            log.info("⏩ Demande URGENTE avec solde insuffisant - passage au manager (dérogation)");
        } else {
            log.info("✅ Solde suffisant - passage au manager");
        }
    }

    private void refuserDemandeEtNotifier(Long demandeId, String motif, Long employeId) {
        DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
        if (demande != null && !"REFUSE".equals(demande.getStatut())) {
            demande.setStatut("REFUSE");
            demande.setMotifRefus(motif);
            demandeRepository.save(demande);
            log.info("Demande {} refusée", demandeId);
        }

        String message = String.format("❌ Votre demande de congé a été refusée. Motif : %s", motif);
        notificationService.createNotification(employeId, message, "ERROR", demandeId);
    }
}
