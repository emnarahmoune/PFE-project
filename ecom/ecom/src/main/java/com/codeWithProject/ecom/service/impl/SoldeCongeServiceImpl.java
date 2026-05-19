package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.SoldeCongeService;
import com.codeWithProject.ecom.service.SoldeDeductionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoldeCongeServiceImpl implements SoldeCongeService {

    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;

    private static final double POURCENTAGE_AUGMENTATION = 2.5;
    private static final int SOLDE_MAX = 999;
    private static final int SOLDE_INITIAL = 25;

    // ===== MÉTHODE EXISTANTE =====

    @Override
    @Transactional
    public void augmenterSoldesMensuels() {
        List<Employe> employesActifs = employeRepository.findByActif(true);
        log.info("Augmentation mensuelle des soldes – {} employé(s) actif(s)", employesActifs.size());

        for (Employe e : employesActifs) {
            Integer soldeActuel = e.getSoldeConges() != null ? e.getSoldeConges() : SOLDE_INITIAL;
            double augmentation = soldeActuel * (POURCENTAGE_AUGMENTATION / 100.0);
            double nouveauSoldeDouble = soldeActuel + augmentation;
            BigDecimal nouveauSolde = BigDecimal.valueOf(nouveauSoldeDouble)
                    .setScale(2, RoundingMode.HALF_UP);
            int nouveauSoldeInt = nouveauSolde.intValue();
            if (nouveauSoldeInt > SOLDE_MAX) {
                nouveauSoldeInt = SOLDE_MAX;
            }
            e.setSoldeConges(nouveauSoldeInt);
            employeRepository.save(e);
            log.debug("Employé {} : solde {} → {}", e.getEmail(), soldeActuel, nouveauSoldeInt);
        }
    }

    // ===== NOUVELLES MÉTHODES =====

    @Override
    public int getSoldeTotal(Long employeId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeId));
        return employe.getSoldeConges() != null ? employe.getSoldeConges() : SOLDE_INITIAL;
    }

    @Override
    public int getJoursPrisDansAnnee(Long employeId, int annee) {
        return demandeCongeRepository.sumJoursOuvresApprouvesAnnee(employeId, annee);
    }

    @Override
    public int getSoldeRestant(Long employeId, int annee) {
        int soldeTotal = getSoldeTotal(employeId);
        int joursPris = getJoursPrisDansAnnee(employeId, annee);
        return soldeTotal - joursPris;
    }

    @Override
    public boolean verifierSoldeSuffisant(Long employeId, int joursDemandes, int annee) {
        int soldeRestant = getSoldeRestant(employeId, annee);
        return soldeRestant >= joursDemandes;
    }

    @Override
    @Transactional
    public SoldeDeductionResult decrementerSoldeConge(Long employeId, int joursDemandes, boolean isUrgent) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeId));

        int soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : SOLDE_INITIAL;
        int annee = LocalDate.now().getYear();
        int soldeRestantReel = getSoldeRestant(employeId, annee);

        log.info("Déduction solde - Employé: {}, Solde actuel: {}, Solde restant réel: {}, Demandé: {}, Urgent: {}",
                employeId, soldeActuel, soldeRestantReel, joursDemandes, isUrgent);

        // Cas 1: Solde suffisant
        if (soldeRestantReel >= joursDemandes) {
            int nouveauSolde = soldeActuel - joursDemandes;
            employe.setSoldeConges(nouveauSolde);
            employeRepository.save(employe);
            log.info("✅ Déduction totale: {} jours déduits, nouveau solde: {}", joursDemandes, nouveauSolde);
            return SoldeDeductionResult.succes(joursDemandes, nouveauSolde);
        }

        // Cas 2: Solde insuffisant mais demande urgente
        if (isUrgent && soldeRestantReel < joursDemandes) {
            int joursDeduits = soldeRestantReel;
            int joursNonCouverts = joursDemandes - soldeRestantReel;
            int nouveauSolde = soldeActuel - joursDeduits;

            employe.setSoldeConges(nouveauSolde);
            employeRepository.save(employe);

            log.info("⚠️ Déduction partielle (urgent): {} jours déduits, {} jours non couverts, nouveau solde: {}",
                    joursDeduits, joursNonCouverts, nouveauSolde);
            return SoldeDeductionResult.partiel(joursDeduits, joursNonCouverts, nouveauSolde);
        }

        // Cas 3: Solde insuffisant et non urgent
        log.warn("❌ Déduction impossible: solde insuffisant et demande non urgente");
        return SoldeDeductionResult.echec();
    }

    @Override
    @Transactional
    public void incrementerSoldeConge(Long employeId, int jours) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + employeId));

        int soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : SOLDE_INITIAL;
        int nouveauSolde = soldeActuel + jours;
        employe.setSoldeConges(nouveauSolde);
        employeRepository.save(employe);

        log.info("✅ Solde augmenté: {} jours ajoutés, nouveau solde: {}", jours, nouveauSolde);
    }
}