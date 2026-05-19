package com.codeWithProject.ecom.service.workflow;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.SoldeCongeService;
import com.codeWithProject.ecom.service.SoldeDeductionResult;
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

    private final SoldeCongeService soldeCongeService;
    private final DemandeCongeRepository demandeRepository;

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

        // Vérifier si déjà refusée
        if ("REFUSE".equals(demande.getStatut()) || "REFUSE_MANAGER".equals(demande.getStatut())) {
            log.info("Demande {} déjà refusée, aucune déduction", demande.getId());
            execution.setVariable("soldeDeduit", true);
            return;
        }

        // Éviter la double déduction
        Boolean dejaDeduit = (Boolean) execution.getVariable("soldeDeduit");
        if (dejaDeduit != null && dejaDeduit) {
            log.info("Solde déjà déduit pour la demande {}", demande.getId());
            return;
        }

        // Seul l'ANNUEL est déduit
        if (!"ANNUEL".equals(demande.getType()) || demande.getEmploye() == null) {
            log.info("Pas de déduction pour le type: {}", demande.getType());
            execution.setVariable("soldeDeduit", true);
            return;
        }

        Long employeId = demande.getEmploye().getId();
        int joursDemandes = demande.getJoursOuvres();
        boolean urgente = Boolean.TRUE.equals(demande.getUrgente());

        // Utilisation du service centralisé
        SoldeDeductionResult result = soldeCongeService.decrementerSoldeConge(employeId, joursDemandes, urgente);

        if (result.isDeductionEffectuee()) {
            execution.setVariable("soldeDeduit", true);
            execution.setVariable("joursDeduits", result.getJoursDeduits());
            execution.setVariable("joursNonCouverts", result.getJoursNonCouverts());

            // Sauvegarder les jours non couverts pour les demandes urgentes
            if (result.getJoursNonCouverts() > 0) {
                demande.setJoursUrgenceNonCouverts(result.getJoursNonCouverts());
                demandeRepository.save(demande);
                log.info("Demande URGENTE: {} jours déduits, {} jours non couverts",
                        result.getJoursDeduits(), result.getJoursNonCouverts());
            } else {
                log.info("✅ Solde déduit: {} jours (nouveau solde: {})",
                        result.getJoursDeduits(), result.getNouveauSolde());
            }

            // Mettre à jour le statut si nécessaire
            if (!"APPROUVE".equals(demande.getStatut())) {
                demande.setStatut("APPROUVE");
                demande.setDateDecision(LocalDate.now());
                demandeRepository.save(demande);
            }
        } else {
            log.warn("⚠️ Déduction impossible pour demande {}", demande.getId());
            execution.setVariable("soldeDeduit", false);
        }
    }
}