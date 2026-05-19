package com.codeWithProject.ecom.scheduling;

import com.codeWithProject.ecom.service.SoldeCongeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SoldeCongeJob {

    private final SoldeCongeService soldeCongeService;

    /**
     * Exécution le 1er jour de chaque mois à 00:00.
     * Augmente le solde de tous les employés actifs de 2,5%.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void augmenterSoldesMensuels() {
        log.info("Début du job d'augmentation mensuelle des soldes de congés");
        try {
            soldeCongeService.augmenterSoldesMensuels();
            log.info("Job terminé avec succès");
        } catch (Exception e) {
            log.error("Erreur lors du job d'augmentation des soldes: {}", e.getMessage(), e);
        }
    }
}