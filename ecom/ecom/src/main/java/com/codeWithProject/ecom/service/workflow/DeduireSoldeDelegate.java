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

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeduireSoldeDelegate implements JavaDelegate {

    private final DemandeCongeRepository demandeRepository;
    private final EmployeRepository employeRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        log.info("=== DÉDUCTION DU SOLDE ===");

        String processInstanceId = execution.getProcessInstanceId();
        DemandeConge demande = demandeRepository.findByProcessInstanceId(processInstanceId).orElse(null);

        if (demande == null) {
            log.warn("Demande non trouvée pour processInstanceId: {}", processInstanceId);
            execution.setVariable("soldeDeduit", false);
            return;
        }

        // Ne pas déduire si la demande est déjà refusée
        if ("REFUSE".equals(demande.getStatut()) || "REFUSE_MANAGER".equals(demande.getStatut())) {
            log.info("Demande {} déjà refusée, aucune déduction", demande.getId());
            execution.setVariable("soldeDeduit", true);
            return;
        }

        Boolean dejaDeduit = (Boolean) execution.getVariable("soldeDeduit");
        if (dejaDeduit != null && dejaDeduit) {
            log.info("Solde déjà déduit pour la demande {}", demande.getId());
            return;
        }

        if (!"ANNUEL".equals(demande.getType()) || demande.getEmploye() == null) {
            log.info("Pas de déduction pour le type de congé: {}", demande.getType());
            execution.setVariable("soldeDeduit", true);
            return;
        }

        Employe employe = demande.getEmploye();
        int joursDemandes = demande.getJoursOuvres();
        int soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : 0;
        boolean urgente = Boolean.TRUE.equals(demande.getUrgente());

        if (urgente && soldeActuel < joursDemandes) {
            int joursDeduits = soldeActuel;
            int joursNonCouverts = joursDemandes - soldeActuel;

            employe.setSoldeConges(0);
            employeRepository.save(employe);

            demande.setJoursUrgenceNonCouverts(joursNonCouverts);
            demandeRepository.save(demande);

            log.info("⏳ Demande URGENTE ID {} : solde insuffisant (besoin {} j, restant {} j). {} j déduits, {} j non couverts.",
                    demande.getId(), joursDemandes, soldeActuel, joursDeduits, joursNonCouverts);
            execution.setVariable("soldeDeduit", true);
        } else if (soldeActuel >= joursDemandes) {
            employe.deduireConges(joursDemandes);
            employeRepository.save(employe);
            log.info("✅ Solde déduit pour l'employé {} : {} jours (nouveau solde: {})",
                    employe.getId(), joursDemandes, employe.getSoldeConges());
            execution.setVariable("soldeDeduit", true);
        } else {
            log.warn("⚠️ Solde insuffisant pour demande non urgente ID {}. La demande ne sera pas approuvée.", demande.getId());
            execution.setVariable("soldeDeduit", false);
            return;
        }

        if (!"APPROUVE".equals(demande.getStatut())) {
            demande.setStatut("APPROUVE");
            demande.setDateDecision(LocalDate.now());
            demandeRepository.save(demande);
            log.info("✅ Demande {} approuvée (fallback) après déduction.", demande.getId());
        }
    }
}