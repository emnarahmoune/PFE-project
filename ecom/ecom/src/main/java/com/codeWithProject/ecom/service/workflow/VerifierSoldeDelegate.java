package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.NotificationService;
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

    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;
    private final RuntimeService runtimeService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== Vérification du solde ===");

        String processInstanceId = execution.getProcessInstanceId();
        String employeIdStr = (String) execution.getVariable("employeId");
        Object nbJoursObj = execution.getVariable("nbJours");
        Integer nbJours = nbJoursObj != null ? ((Number) nbJoursObj).intValue() : 0;
        Long demandeId = execution.getVariable("demandeId") != null ?
                Long.valueOf(execution.getVariable("demandeId").toString()) : null;

        Boolean urgente = (Boolean) execution.getVariable("urgente");
        if (urgente == null) urgente = false;

        Long employeId = Long.valueOf(employeIdStr);
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeId));

        Integer soldeTotal = employe.getSoldeConges() != null ? employe.getSoldeConges() : 25;
        int annee = LocalDate.now().getYear();
        int joursPris = demandeRepository.sumJoursOuvresApprouvesAnnee(employeId, annee);
        int soldeRestant = soldeTotal - joursPris;
        boolean soldeSuffisant = soldeRestant >= nbJours;

        execution.setVariable("soldeSuffisant", soldeSuffisant);
        execution.setVariable("soldeActuel", soldeRestant);
        execution.setVariable("nbJours", nbJours);

        log.info("Employé ID: {}, Solde total: {}, Pris: {}, Restant: {}, Demandé: {}, Suffisant: {}, Urgente: {}",
                employeId, soldeTotal, joursPris, soldeRestant, nbJours, soldeSuffisant, urgente);

        if (!soldeSuffisant && !urgente) {
            log.info("❌ Demande {} NON URGENTE et solde insuffisant -> REFUS AUTOMATIQUE", demandeId);
            String motif = String.format("Solde de congés insuffisant (solde restant: %d jours, demandé: %d jours)",
                    soldeRestant, nbJours);
            refuserDemandeEtNotifier(demandeId, motif, employeId);

            // Supprimer l'instance de processus pour éviter toute modification ultérieure
            try {
                runtimeService.deleteProcessInstance(processInstanceId, "Refus automatique pour solde insuffisant", true, true);
                log.info("Instance de processus {} supprimée après refus", processInstanceId);
            } catch (Exception e) {
                log.error("Erreur lors de la suppression de l'instance {}: {}", processInstanceId, e.getMessage());
            }
            // Ne pas continuer l'exécution du workflow
            return;
        } else if (!soldeSuffisant && urgente) {
            log.info("⏩ Demande URGENTE avec solde insuffisant -> passage au manager");
        } else {
            log.info("✅ Solde suffisant -> passage au manager");
        }
        log.info("✅ Vérification du solde terminée (suite normale)");
    }

    @Transactional
    public void refuserDemandeEtNotifier(Long demandeId, String motif, Long employeId) {
        DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
        if (demande != null && !"REFUSE".equals(demande.getStatut())) {
            demande.setStatut("REFUSE");
            demande.setMotifRefus(motif);
            demandeRepository.saveAndFlush(demande);
            log.info("❌ Demande {} refusée automatiquement - Solde insuffisant (statut mis à REFUSE)", demandeId);
        } else {
            log.warn("Demande {} introuvable ou déjà refusée", demandeId);
        }

        String message = String.format("❌ Votre demande de congé a été refusée automatiquement. Motif : %s", motif);
        notificationService.createNotification(employeId, message, "ERROR", demandeId);
        log.info("📧 Notification de refus envoyée à l'employé {}", employeId);
    }
}