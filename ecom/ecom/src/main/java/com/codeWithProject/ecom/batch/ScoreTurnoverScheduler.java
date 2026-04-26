package com.codeWithProject.ecom.batch;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.ParametreService;
import com.codeWithProject.ecom.service.ScoreTurnoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreTurnoverScheduler {

    private final EmployeRepository employeRepository;
    private final ScoreTurnoverService scoreTurnoverService;
    private final ParametreService parametreService;

    // Expression cron lue dynamiquement depuis la base
    @Scheduled(cron = "#{@parametreService.getCronBatchTurnover()}")
    @Transactional
    public void recalculerScoresTurnover() {
        log.info("Début du recalcul automatique des scores de turnover");

        Long systemeBIId = parametreService.getLong("batch.turnover.systeme_bi_id");
        if (systemeBIId == null) {
            log.error("Paramètre 'batch.turnover.systeme_bi_id' non défini. Abandon du batch.");
            return;
        }

        List<Employe> employes = employeRepository.findAll();
        for (Employe e : employes) {
            try {
                scoreTurnoverService.calculerScorePourEmploye(e.getId(), systemeBIId);
                log.info("Score recalculé pour employé {}", e.getId());
            } catch (Exception ex) {
                log.error("Erreur pour employé {} : {}", e.getId(), ex.getMessage());
            }
        }
        log.info("Fin du recalcul des scores de turnover");
    }
}