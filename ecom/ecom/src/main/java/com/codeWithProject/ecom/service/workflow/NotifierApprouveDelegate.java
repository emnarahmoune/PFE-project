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

@Component
@RequiredArgsConstructor
@Slf4j
public class NotifierApprouveDelegate implements JavaDelegate {

    private final DemandeCongeRepository demandeRepository;
    private final EmployeRepository employeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        log.info("=== NOTIFICATION APPROBATION ===");

        String processInstanceId = execution.getProcessInstanceId();
        DemandeConge demande = demandeRepository.findByProcessInstanceId(processInstanceId)
                .orElse(null);

        if (demande == null) {
            log.warn("Demande non trouvée pour processInstanceId: {}", processInstanceId);
            return;
        }

        // Éviter les doublons d’exécution du delegate
        Boolean notificationEnvoyee = (Boolean) execution.getVariable("notificationApprobationEnvoyee");
        if (notificationEnvoyee != null && notificationEnvoyee) {
            log.info("Notification d'approbation déjà envoyée pour la demande {}", demande.getId());
            return;
        }

        // Ne PAS appeler demande.valider() – l’état a déjà été changé par le service métier

        // Déduire le solde si nécessaire (et si non déjà fait)
        Boolean soldeDejaDeduit = (Boolean) execution.getVariable("soldeDeduit");
        if (soldeDejaDeduit == null || !soldeDejaDeduit) {
            if ("ANNUEL".equals(demande.getType()) && demande.getEmploye() != null) {
                Employe employe = demande.getEmploye();
                int jours = demande.getJoursOuvres();
                if (employe.getSoldeConges() != null && employe.getSoldeConges() >= jours) {
                    employe.deduireConges(jours);
                    employeRepository.save(employe);
                    log.info("✅ Solde déduit pour l'employé {} : {} jours", employe.getId(), jours);
                    execution.setVariable("soldeDeduit", true);
                }
            }
        }

        // Créer la notification (toujours)
        String message = String.format("✅ Votre demande de congé du %s au %s a été approuvée !",
                demande.getDateDebut(), demande.getDateFin());
        notificationService.createNotification(
                demande.getEmploye().getId(), message, "SUCCESS", demande.getId());

        execution.setVariable("notificationApprobationEnvoyee", true);
        log.info("✅ Notification d'approbation créée pour la demande {}", demande.getId());
    }
}