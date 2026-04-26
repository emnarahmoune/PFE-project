package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import java.time.LocalDate;

public interface AbsenteismeService {
    // Calcul du taux sur une année fixe
    double calculerTaux(Long employeId, int annee);

    // Sauvegarde d'un indicateur annuel
    IndicateurRHDTO calculerEtSauvegarder(Long employeId, int annee);

    // Sauvegarde sur une période personnalisée (glissante)
    IndicateurRHDTO calculerEtSauvegarderSurPeriode(Long employeId, LocalDate debut, LocalDate fin, String periode);
}