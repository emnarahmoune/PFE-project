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
        // ✅ Correction: findByProcessInstanceId retourne Optional
        DemandeConge demande = demandeRepository.findByProcessInstanceId(processInstanceId)
                .orElse(null);

        if (demande == null) {
            log.warn("Demande non trouvée pour processInstanceId: {}", processInstanceId);
            return;
        }

        // ✅ Vérifier si déjà approuvée (anti-double)
        if ("APPROUVEE".equals(demande.getStatut())) {
            log.info("Demande {} déjà approuvée, traitement ignoré", demande.getId());
            return;
        }

        // Valider la demande
        demande.valider();

        // ✅ Vérifier si le solde n'a pas déjà été déduit par DeduireSoldeDelegate
        Boolean soldeDejaDeduit = (Boolean) execution.getVariable("soldeDeduit");

        if (soldeDejaDeduit == null || !soldeDejaDeduit) {
            // Déduire les jours du solde si c'est un congé annuel
            if ("ANNUEL".equals(demande.getType()) && demande.getEmploye() != null) {
                Employe employe = demande.getEmploye();
                int jours = demande.getJoursOuvres();
                if (employe.getSoldeConges() != null && employe.getSoldeConges() >= jours) {
                    employe.deduireConges(jours);
                    employeRepository.save(employe);
                    log.info("✅ Solde déduit pour l'employé {} : {} jours (nouveau solde: {})",
                            employe.getId(), jours, employe.getSoldeConges());
                    execution.setVariable("soldeDeduit", true);
                }
            }
        } else {
            log.info("Solde déjà déduit par DeduireSoldeDelegate, pas de double déduction");
        }

        demandeRepository.save(demande);

        // Créer une notification
        String message = String.format("✅ Votre demande de congé du %s au %s a été approuvée !",
                demande.getDateDebut(), demande.getDateFin());
        notificationService.createNotification(
                demande.getEmploye().getId(), message, "SUCCESS", demande.getId());

        log.info("✅ Demande {} approuvée et notification créée", demande.getId());
    }
}