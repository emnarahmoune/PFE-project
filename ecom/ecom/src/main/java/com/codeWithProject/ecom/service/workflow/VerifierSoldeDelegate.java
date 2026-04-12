package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerifierSoldeDelegate implements JavaDelegate {

    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== Vérification du solde ===");

        // Récupérer les variables
        String employeIdStr = (String) execution.getVariable("employeId");
        Object nbJoursObj = execution.getVariable("nbJours");
        Integer nbJours = nbJoursObj != null ? ((Number) nbJoursObj).intValue() : 0;
        Long demandeId = execution.getVariable("demandeId") != null ?
                Long.valueOf(execution.getVariable("demandeId").toString()) : null;

        Long employeId = Long.valueOf(employeIdStr);
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeId));

        Integer soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : 25;
        boolean soldeSuffisant = soldeActuel >= nbJours;

        execution.setVariable("soldeSuffisant", soldeSuffisant);
        execution.setVariable("soldeActuel", soldeActuel);
        execution.setVariable("nbJours", nbJours);

        log.info("Employé ID: {}, Solde: {}, Demandé: {}, Suffisant: {}",
                employeId, soldeActuel, nbJours, soldeSuffisant);

        // ❌ SOLDE INSUFFISANT - Lancer une erreur BPMN
        if (!soldeSuffisant && demandeId != null) {
            String motif = "Solde de congés insuffisant (solde: " + soldeActuel + " jours, demandé: " + nbJours + " jours)";

            // Mettre à jour la demande en base
            DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
            if (demande != null) {
                demande.setStatut("REFUSE");
                demande.setMotifRefus(motif);
                demandeRepository.save(demande);
                log.info("❌ Demande {} refusée - Solde insuffisant", demandeId);
            }

            // Créer une notification
            String message = String.format("❌ Votre demande de congé a été refusée automatiquement. Motif : %s", motif);
            notificationService.createNotification(employeId, message, "ERROR", demandeId);
            log.info("📧 Notification de refus envoyée à l'employé {}", employeId);

            // LANCER L'ERREUR BPMN
            throw new BpmnError("SOLDE_INSUFFISANT", motif);
        }

        log.info("✅ Solde suffisant, poursuite du workflow");
    }
}