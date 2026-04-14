package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeduireSoldeDelegate implements JavaDelegate {

    private final DemandeCongeRepository demandeRepository;
    private final EmployeRepository employeRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        log.info("=== DÉDUCTION DU SOLDE ===");

        String processInstanceId = execution.getProcessInstanceId();
        DemandeConge demande = demandeRepository.findByProcessInstanceId(processInstanceId)
                .orElse(null);

        if (demande == null) {
            log.warn("Demande non trouvée pour processInstanceId: {}", processInstanceId);
            execution.setVariable("soldeDeduit", false);
            return;
        }

        // Correction : le statut après approbation est "APPROUVE" (un seul E)
        if (!"APPROUVE".equals(demande.getStatut())) {
            log.warn("Demande {} non approuvée (statut={}), déduction ignorée",
                    demande.getId(), demande.getStatut());
            execution.setVariable("soldeDeduit", false);
            return;
        }

        if ("ANNUEL".equals(demande.getType()) && demande.getEmploye() != null) {
            Employe employe = demande.getEmploye();
            int jours = demande.getJoursOuvres();
            if (employe.getSoldeConges() != null && employe.getSoldeConges() >= jours) {
                employe.deduireConges(jours);
                employeRepository.save(employe);
                log.info("✅ Solde déduit pour l'employé {} : {} jours (nouveau solde: {})",
                        employe.getId(), jours, employe.getSoldeConges());
                execution.setVariable("soldeDeduit", true);
            } else {
                log.warn("⚠️ Solde insuffisant lors de la déduction pour la demande {}", demande.getId());
                execution.setVariable("soldeDeduit", false);
            }
        } else {
            log.info("Pas de déduction pour le type de congé: {}", demande.getType());
            execution.setVariable("soldeDeduit", true);
        }
    }
}