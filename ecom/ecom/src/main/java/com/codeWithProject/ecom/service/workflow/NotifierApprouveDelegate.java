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
public class NotifierApprouveDelegate implements JavaDelegate {

    private final DemandeCongeRepository demandeRepository;
    private final NotificationService notificationService;   // EmployeRepository retiré

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== NOTIFICATION APPROBATION ===");
        String processInstanceId = execution.getProcessInstanceId();
        DemandeConge demande = demandeRepository.findByProcessInstanceId(processInstanceId).orElse(null);
        if (demande == null) {
            log.warn("Demande non trouvée pour processInstanceId: {}", processInstanceId);
            return;
        }

        Boolean notificationEnvoyee = (Boolean) execution.getVariable("notificationApprobationEnvoyee");
        if (notificationEnvoyee != null && notificationEnvoyee) {
            log.info("Notification d'approbation déjà envoyée pour la demande {}", demande.getId());
            return;
        }

        // La déduction du solde est déjà faite dans DeduireSoldeDelegate
        String message = String.format("✅ Votre demande de congé du %s au %s a été approuvée !",
                demande.getDateDebut(), demande.getDateFin());
        notificationService.createNotification(demande.getEmploye().getId(), message, "SUCCESS", demande.getId());

        execution.setVariable("notificationApprobationEnvoyee", true);
        log.info("✅ Notification d'approbation créée pour la demande {}", demande.getId());
    }
}