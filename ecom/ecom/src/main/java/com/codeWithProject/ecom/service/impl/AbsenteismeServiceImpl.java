package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.AbsenteismeService;
import com.codeWithProject.ecom.service.IndicateurRHService;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class AbsenteismeServiceImpl implements AbsenteismeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final IndicateurRHService indicateurRHService;

    public AbsenteismeServiceImpl(DemandeCongeRepository demandeCongeRepository,
                                  @Lazy IndicateurRHService indicateurRHService) {
        this.demandeCongeRepository = demandeCongeRepository;
        this.indicateurRHService = indicateurRHService;
    }

    @Value("${rh.jours-ouvres-theoriques.annee:220}")
    private int joursOuvresTheoriques;

    @Override
    public double calculerTaux(Long employeId, int annee) {
        int joursAbsence = demandeCongeRepository.sumJoursAbsence(employeId, annee);
        return (joursAbsence * 100.0) / joursOuvresTheoriques;
    }

    @Override
    @Transactional
    public IndicateurRHDTO calculerEtSauvegarder(Long employeId, int annee) {
        double taux = calculerTaux(employeId, annee);
        log.info("Taux d'absentéisme annuel ({}): {}% pour employé {}", annee, taux, employeId);

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("ABSENTEISME")
                .valeur(taux)
                .dateCalcul(LocalDate.now())
                .periode("ANNUEL")
                .annee(annee)
                .employeId(employeId)
                .build();
        return indicateurRHService.create(dto);
    }

    @Override
    @Transactional
    public IndicateurRHDTO calculerEtSauvegarderSurPeriode(Long employeId, LocalDate debut, LocalDate fin, String periode) {
        int joursAbsence = demandeCongeRepository.sumJoursAbsenceEntreDates(employeId, debut, fin);
        long joursOuvresPeriode = ChronoUnit.DAYS.between(debut, fin.plusDays(1)) * 5 / 7; // approximation
        double taux = (joursAbsence * 100.0) / joursOuvresPeriode;

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("ABSENTEISME")
                .valeur(taux)
                .dateCalcul(LocalDate.now())
                .periode(periode)
                .annee(debut.getYear())
                .mois(debut.getMonthValue())
                .employeId(employeId)
                .build();
        return indicateurRHService.create(dto);
    }
}