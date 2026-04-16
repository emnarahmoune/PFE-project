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

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== Vérification du solde ===");

        String employeIdStr = (String) execution.getVariable("employeId");
        Object nbJoursObj = execution.getVariable("nbJours");
        Integer nbJours = nbJoursObj != null ? ((Number) nbJoursObj).intValue() : 0;
        Long demandeId = execution.getVariable("demandeId") != null ?
                Long.valueOf(execution.getVariable("demandeId").toString()) : null;

        Long employeId = Long.valueOf(employeIdStr);
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeId));

        // Solde total annuel
        Integer soldeTotal = employe.getSoldeConges() != null ? employe.getSoldeConges() : 25;
        int annee = LocalDate.now().getYear();

        // Jours déjà approuvés dans l'année (congés consommés)
        int joursPris = demandeRepository.sumJoursOuvresApprouvesAnnee(employeId, annee);
        int soldeRestant = soldeTotal - joursPris;

        boolean soldeSuffisant = soldeRestant >= nbJours;

        execution.setVariable("soldeSuffisant", soldeSuffisant);
        execution.setVariable("soldeActuel", soldeRestant);
        execution.setVariable("nbJours", nbJours);

        log.info("Employé ID: {}, Solde total: {}, Pris: {}, Restant: {}, Demandé: {}, Suffisant: {}",
                employeId, soldeTotal, joursPris, soldeRestant, nbJours, soldeSuffisant);

        if (!soldeSuffisant && demandeId != null) {
            String motif = String.format("Solde de congés insuffisant (solde restant: %d jours, demandé: %d jours)",
                    soldeRestant, nbJours);
            refuserDemandeEtNotifier(demandeId, motif, employeId);
        }

        log.info("✅ Solde vérifié, poursuite du workflow");
    }

    @Transactional
    public void refuserDemandeEtNotifier(Long demandeId, String motif, Long employeId) {
        DemandeConge demande = demandeRepository.findById(demandeId).orElse(null);
        if (demande != null && !"REFUSE".equals(demande.getStatut())) {
            demande.setStatut("REFUSE");
            demande.setMotifRefus(motif);
            demandeRepository.save(demande);
            log.info("❌ Demande {} refusée - Solde insuffisant", demandeId);
        }

        String message = String.format("❌ Votre demande de congé a été refusée automatiquement. Motif : %s", motif);
        notificationService.createNotification(employeId, message, "ERROR", demandeId);
        log.info("📧 Notification de refus envoyée à l'employé {}", employeId);
    }
}