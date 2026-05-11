package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.IndicateurRH;
import com.codeWithProject.ecom.entity.ScoreTurnover;
import com.codeWithProject.ecom.entity.SystemeBI;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.EvaluationRepository;
import com.codeWithProject.ecom.repository.IndicateurRHRepository;
import com.codeWithProject.ecom.repository.ScoreTurnoverRepository;
import com.codeWithProject.ecom.repository.SystemeBIRepository;
import com.codeWithProject.ecom.service.ParametreService;
import com.codeWithProject.ecom.service.ScoreTurnoverService;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.ScoreTurnoverMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreTurnoverServiceImpl implements ScoreTurnoverService {

    private final ScoreTurnoverRepository scoreTurnoverRepository;
    private final EmployeRepository employeRepository;
    private final SystemeBIRepository systemeBIRepository;
    private final EvaluationRepository evaluationRepository;
    private final IndicateurRHRepository indicateurRHRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final ParametreService parametreService;
    private final ScoreTurnoverMapper mapper;

    @Override
    public List<ScoreTurnoverDTO> findAll() {
        return scoreTurnoverRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public Page<ScoreTurnoverDTO> findAll(Pageable pageable) {
        Page<ScoreTurnover> page = scoreTurnoverRepository.findAll(pageable);

        List<ScoreTurnoverDTO> dtos = page.getContent()
                .stream()
                .map(mapper::toDto)
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public Optional<ScoreTurnoverDTO> findById(Long id) {
        return scoreTurnoverRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public List<ScoreTurnoverDTO> findByEmployeId(Long employeId) {
        return scoreTurnoverRepository.findByEmployeId(employeId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<ScoreTurnoverDTO> findHistoriqueEmploye(Long employeId) {
        return scoreTurnoverRepository.findHistoriqueEmploye(employeId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public Optional<ScoreTurnoverDTO> findDernierScoreEmploye(Long employeId) {
        return scoreTurnoverRepository.findDernierScoreEmploye(employeId)
                .stream()
                .findFirst()
                .map(mapper::toDto);
    }

    @Override
    public List<ScoreTurnoverDTO> findByNiveauRisque(String niveauRisque) {
        return scoreTurnoverRepository.findByNiveauRisque(niveauRisque)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<ScoreTurnoverDTO> findScoresRisques() {
        return scoreTurnoverRepository.findScoresRisques()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<ScoreTurnoverDTO> findScoresCritiques() {
        return scoreTurnoverRepository.findScoresCritiques()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public List<ScoreTurnoverDTO> findDerniersScores() {
        SystemeBI systemeBI = getSystemeBIActif();

        List<Employe> employes = employeRepository.findAll();

        for (Employe employe : employes) {
            try {
                calculerScorePourEmploye(employe.getId(), systemeBI.getId());
            } catch (Exception e) {
                log.warn(
                        "Score turnover non recalculé pour employé {} : {}",
                        employe.getId(),
                        e.getMessage()
                );
            }
        }

        return scoreTurnoverRepository.findDerniersScores()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ScoreTurnoverDTO calculerScorePourEmploye(Long employeId, Long systemeBIId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        SystemeBI systemeBI = systemeBIId != null
                ? systemeBIRepository.findById(systemeBIId)
                    .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", systemeBIId))
                : getSystemeBIActif();

        double poidsAnciennete = parametreService.getDouble("score.turnover.poids.anciennete", 0.20);
        double poidsSalaire = parametreService.getDouble("score.turnover.poids.salaire", 0.25);
        double poidsPerformance = parametreService.getDouble("score.turnover.poids.performance", 0.20);
        double poidsFormation = parametreService.getDouble("score.turnover.poids.formation", 0.15);
        double poidsAbsenteisme = parametreService.getDouble("score.turnover.poids.absenteisme", 0.20);

        double totalPoids = poidsAnciennete
                + poidsSalaire
                + poidsPerformance
                + poidsFormation
                + poidsAbsenteisme;

        if (totalPoids <= 0) {
            totalPoids = 1.0;
        }

        poidsAnciennete = poidsAnciennete / totalPoids;
        poidsSalaire = poidsSalaire / totalPoids;
        poidsPerformance = poidsPerformance / totalPoids;
        poidsFormation = poidsFormation / totalPoids;
        poidsAbsenteisme = poidsAbsenteisme / totalPoids;

        double scoreAnciennete = calculerScoreAnciennete(employe.getDateEmbauche());
        double scoreSalaire = calculerScoreSalaire(employe.getSalaire());
        double scorePerformance = calculerScorePerformance(employeId);
        double scoreFormation = calculerScoreFormation(employeId);
        double scoreAbsenteisme = calculerScoreAbsenteisme(employeId);

        double scoreGlobal =
                scoreAnciennete * poidsAnciennete
                        + scoreSalaire * poidsSalaire
                        + scorePerformance * poidsPerformance
                        + scoreFormation * poidsFormation
                        + scoreAbsenteisme * poidsAbsenteisme;

        scoreGlobal = round2(clamp(scoreGlobal, 0, 100));

        String niveau = determinerNiveauRisque(scoreGlobal);

        String facteurs = identifierFacteurs(
                scoreAnciennete,
                scoreSalaire,
                scorePerformance,
                scoreFormation,
                scoreAbsenteisme
        );

        String actions = recommanderActions(
                scoreAnciennete,
                scoreSalaire,
                scorePerformance,
                scoreFormation,
                scoreAbsenteisme
        );

        LocalDate today = LocalDate.now();

        ScoreTurnover score = scoreTurnoverRepository
                .findByEmployeIdAndDatePrediction(employeId, today)
                .orElseGet(ScoreTurnover::new);

        score.setEmploye(employe);
        score.setSystemeBI(systemeBI);
        score.setScore(scoreGlobal);
        score.setNiveauRisque(niveau);
        score.setDatePrediction(today);
        score.setPeriodePrediction("6_MOIS");
        score.setFacteursPrincipaux(facteurs);
        score.setActionRecommandee(actions);
        score.setConfianceModele(parametreService.getDouble("score.turnover.confiance.modele", 0.88));
        score.setVersionModele(parametreService.getValeur("score.turnover.version.modele", "v3.0-dynamique"));
        score.setScoreAnciennete(round2(scoreAnciennete));
        score.setScoreSalaire(round2(scoreSalaire));
        score.setScorePerformance(round2(scorePerformance));
        score.setScoreFormation(round2(scoreFormation));
        score.setScoreAbsenteisme(round2(scoreAbsenteisme));

        ScoreTurnover saved = scoreTurnoverRepository.save(score);

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<ScoreTurnoverDTO> calculerScoresPourTousEmployes(Long systemeBIId) {
        SystemeBI systemeBI = systemeBIId != null
                ? systemeBIRepository.findById(systemeBIId)
                    .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", systemeBIId))
                : getSystemeBIActif();

        List<Employe> employes = employeRepository.findAll();
        List<ScoreTurnoverDTO> resultats = new ArrayList<>();

        for (Employe employe : employes) {
            try {
                resultats.add(calculerScorePourEmploye(employe.getId(), systemeBI.getId()));
            } catch (Exception e) {
                log.error(
                        "Erreur calcul score turnover employé {} : {}",
                        employe.getId(),
                        e.getMessage()
                );
            }
        }

        return resultats;
    }

    @Override
    public ScoreTurnoverDTO predireRisque(Long employeId, Long systemeBIId) {
        return calculerScorePourEmploye(employeId, systemeBIId);
    }

    @Override
    public void delete(Long id) {
        if (!scoreTurnoverRepository.existsById(id)) {
            throw new ResourceNotFoundException("ScoreTurnover", id);
        }

        scoreTurnoverRepository.deleteById(id);
    }

    @Override
    public void deleteScoresObsoletes(int jours) {
        LocalDate dateLimite = LocalDate.now().minusDays(jours);
        List<ScoreTurnover> obsoletes = scoreTurnoverRepository.findScoresObsoletes(dateLimite);

        scoreTurnoverRepository.deleteAll(obsoletes);

        log.info("{} scores obsolètes supprimés", obsoletes.size());
    }

    @Override
    public Map<String, Long> getRepartitionRisques() {
        return scoreTurnoverRepository.repartitionRisquesActuels()
                .stream()
                .collect(Collectors.toMap(
                        arr -> arr[0] == null ? "INCONNU" : String.valueOf(arr[0]),
                        arr -> ((Number) arr[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Double getScoreMoyenActuel() {
        Double moyenne = scoreTurnoverRepository.scoreMoyenActuel();
        return moyenne == null ? 0.0 : round2(moyenne);
    }

    @Override
    public Map<String, Double> getScoreMoyenParDepartement() {
        return scoreTurnoverRepository.scoreMoyenParDepartement()
                .stream()
                .collect(Collectors.toMap(
                        arr -> arr[0] == null ? "Non défini" : String.valueOf(arr[0]),
                        arr -> arr[1] == null ? 0.0 : round2(((Number) arr[1]).doubleValue()),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }

    @Override
    public List<ScoreTurnoverDTO> findScoresAvecActions() {
        return scoreTurnoverRepository.findAvecActionsRecommandees()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public Map<String, Object> getStatsTableauBord() {
        List<ScoreTurnoverDTO> derniers = findDerniersScores();

        long total = derniers.size();

        double moyenne = total == 0
                ? 0.0
                : derniers.stream()
                    .mapToDouble(s -> s.getScore() == null ? 0.0 : s.getScore())
                    .average()
                    .orElse(0.0);

        double min = derniers.stream()
                .mapToDouble(s -> s.getScore() == null ? 0.0 : s.getScore())
                .min()
                .orElse(0.0);

        double max = derniers.stream()
                .mapToDouble(s -> s.getScore() == null ? 0.0 : s.getScore())
                .max()
                .orElse(0.0);

        long critique = derniers.stream()
                .filter(s -> "CRITIQUE".equalsIgnoreCase(s.getNiveauRisque()))
                .count();

        long eleve = derniers.stream()
                .filter(s -> "ELEVE".equalsIgnoreCase(s.getNiveauRisque()))
                .count();

        long moyen = derniers.stream()
                .filter(s -> "MOYEN".equalsIgnoreCase(s.getNiveauRisque()))
                .count();

        long faible = derniers.stream()
                .filter(s -> "FAIBLE".equalsIgnoreCase(s.getNiveauRisque()))
                .count();

        double confianceMoyenne = derniers.stream()
                .mapToDouble(s -> s.getConfianceModele() == null ? 0.0 : s.getConfianceModele())
                .average()
                .orElse(0.0);

        return Map.of(
                "totalScores", total,
                "scoreMoyen", round2(moyenne),
                "scoreMin", round2(min),
                "scoreMax", round2(max),
                "risqueCritique", critique,
                "risqueEleve", eleve,
                "risqueMoyen", moyen,
                "risqueFaible", faible,
                "confianceMoyenne", round2(confianceMoyenne)
        );
    }

    @Override
    public boolean hasScoreRecent(Long employeId, int jours) {
        LocalDate dateLimite = LocalDate.now().minusDays(jours);
        return scoreTurnoverRepository.hasScoreRecent(employeId, dateLimite);
    }

    private double calculerScoreAnciennete(LocalDate dateEmbauche) {
        if (dateEmbauche == null) {
            return 50.0;
        }

        long mois = ChronoUnit.MONTHS.between(dateEmbauche, LocalDate.now());

        if (mois < 6) {
            return 85.0;
        }

        if (mois < 12) {
            return 65.0;
        }

        if (mois < 36) {
            return 35.0;
        }

        if (mois < 60) {
            return 10.0;
        }

        if (mois < 96) {
            return 25.0;
        }

        return 45.0;
    }

    private double calculerScoreSalaire(BigDecimal salaire) {
        if (salaire == null || salaire.compareTo(BigDecimal.ZERO) <= 0) {
            return 70.0;
        }

        double salaireValue = salaire.doubleValue();

        double salaireMinDt = parametreService.getDouble("score.salaire.minimum.dt", 900.0);
        double seuilBas = parametreService.getDouble("score.salaire.seuil.bas.dt", 1200.0);
        double seuilMoyen = parametreService.getDouble("score.salaire.seuil.moyen.dt", 1800.0);
        double seuilBon = parametreService.getDouble("score.salaire.seuil.bon.dt", 2500.0);

        if (salaireValue <= salaireMinDt) {
            return 100.0;
        }

        if (salaireValue <= seuilBas) {
            return 80.0;
        }

        if (salaireValue <= seuilMoyen) {
            return 50.0;
        }

        if (salaireValue <= seuilBon) {
            return 25.0;
        }

        return 5.0;
    }

    private double calculerScorePerformance(Long employeId) {
        Double moyenne = evaluationRepository.moyenneEvaluationAnnuelle(
                employeId,
                LocalDate.now().getYear()
        );

        double noteMax = parametreService.getDouble("score.performance.note.max", 10.0);

        if (moyenne == null) {
            return 40.0;
        }

        moyenne = clamp(moyenne, 0, noteMax);

        double risque = ((noteMax - moyenne) / noteMax) * 100.0;

        return clamp(risque, 0, 100);
    }

    private double calculerScoreFormation(Long employeId) {
        List<EmployeFormation> formations = employeFormationRepository.findByEmployeId(employeId);

        if (formations == null || formations.isEmpty()) {
            return 100.0;
        }

        long terminees = formations.stream()
                .filter(f ->
                        "TERMINEE".equalsIgnoreCase(String.valueOf(f.getStatut()))
                                || "TERMINE".equalsIgnoreCase(String.valueOf(f.getStatut()))
                                || (f.getProgression() != null && f.getProgression() >= 100)
                )
                .count();

        double progressionMoyenne = formations.stream()
                .mapToDouble(f -> f.getProgression() == null ? 0.0 : f.getProgression())
                .average()
                .orElse(0.0);

        if (terminees >= 3) {
            return 5.0;
        }

        if (terminees >= 1 && progressionMoyenne >= 70) {
            return 15.0;
        }

        if (progressionMoyenne >= 50) {
            return 35.0;
        }

        if (progressionMoyenne >= 20) {
            return 60.0;
        }

        return 80.0;
    }

    private double calculerScoreAbsenteisme(Long employeId) {
        Optional<IndicateurRH> dernierAbs = indicateurRHRepository
                .findTopByEmployeIdAndTypeOrderByDateCalculDesc(employeId, "ABSENTEISME");

        if (dernierAbs.isEmpty() || dernierAbs.get().getValeur() == null) {
            return 0.0;
        }

        double taux = dernierAbs.get().getValeur();

        double seuilMoyen = parametreService.getDouble("score.absenteisme.seuil.moyen", 5.0);
        double seuilEleve = parametreService.getDouble("score.absenteisme.seuil.eleve", 10.0);

        if (taux >= seuilEleve) {
            return 100.0;
        }

        if (taux >= seuilMoyen) {
            return 60.0;
        }

        return clamp((taux / seuilMoyen) * 30.0, 0, 30);
    }

    private String determinerNiveauRisque(double score) {
        double seuilFaibleMoyen = parametreService.getDouble("score.turnover.seuil.faible.moyen", 25.0);
        double seuilMoyenEleve = parametreService.getDouble("score.turnover.seuil.moyen.eleve", 50.0);
        double seuilEleveCritique = parametreService.getDouble("score.turnover.seuil.eleve.critique", 75.0);

        if (score < seuilFaibleMoyen) {
            return "FAIBLE";
        }

        if (score < seuilMoyenEleve) {
            return "MOYEN";
        }

        if (score < seuilEleveCritique) {
            return "ELEVE";
        }

        return "CRITIQUE";
    }

    private String identifierFacteurs(
            double anciennete,
            double salaire,
            double performance,
            double formation,
            double absenteisme
    ) {
        List<String> facteurs = new ArrayList<>();

        if (salaire >= 80) {
            facteurs.add("Salaire proche ou inférieur au minimum de référence");
        } else if (salaire >= 50) {
            facteurs.add("Salaire relativement bas");
        }

        if (performance >= 70) {
            facteurs.add("Performance faible");
        } else if (performance >= 45) {
            facteurs.add("Performance moyenne");
        }

        if (formation >= 80) {
            facteurs.add("Manque de formations ou faible progression");
        } else if (formation >= 50) {
            facteurs.add("Progression formation insuffisante");
        }

        if (absenteisme >= 80) {
            facteurs.add("Absentéisme élevé");
        } else if (absenteisme >= 50) {
            facteurs.add("Absentéisme moyen");
        }

        if (anciennete >= 70) {
            facteurs.add("Période d’intégration sensible");
        } else if (anciennete >= 40) {
            facteurs.add("Risque lié à l’ancienneté ou au manque d’évolution");
        }

        if (facteurs.isEmpty()) {
            return "Aucun facteur critique identifié";
        }

        return String.join("; ", facteurs);
    }

    private String recommanderActions(
            double anciennete,
            double salaire,
            double performance,
            double formation,
            double absenteisme
    ) {
        List<String> actions = new ArrayList<>();

        if (salaire >= 80) {
            actions.add("- Revoir la rémunération ou proposer des avantages");
        } else if (salaire >= 50) {
            actions.add("- Étudier une évolution salariale progressive");
        }

        if (formation >= 70) {
            actions.add("- Proposer un plan de formation personnalisé");
        } else if (formation >= 50) {
            actions.add("- Suivre la progression des formations en cours");
        }

        if (performance >= 70) {
            actions.add("- Prévoir un entretien manager et un plan d’accompagnement");
        } else if (performance >= 45) {
            actions.add("- Mettre en place un coaching ou des objectifs intermédiaires");
        }

        if (absenteisme >= 60) {
            actions.add("- Organiser un entretien RH pour comprendre les causes d’absence");
        }

        if (anciennete >= 70) {
            actions.add("- Renforcer l’onboarding et le suivi d’intégration");
        } else if (anciennete >= 40) {
            actions.add("- Discuter des opportunités d’évolution ou de mobilité interne");
        }

        if (actions.isEmpty()) {
            actions.add("- Maintenir le suivi régulier");
        }

        return String.join("\n", actions);
    }

    private SystemeBI getSystemeBIActif() {
        return systemeBIRepository.findAll()
                .stream()
                .max(Comparator.comparing(SystemeBI::getId))
                .orElseThrow(() -> new RuntimeException("Aucun système BI trouvé. Créez au moins un SystemeBI."));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}