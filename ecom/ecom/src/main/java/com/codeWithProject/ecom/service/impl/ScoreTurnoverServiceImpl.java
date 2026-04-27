package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final ParametreService parametreService;
    private final ScoreTurnoverMapper mapper;

    // ==================== MÉTHODES CRUD ====================

    @Override
    public List<ScoreTurnoverDTO> findAll() {
        return scoreTurnoverRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ScoreTurnoverDTO> findAll(Pageable pageable) {
        Page<ScoreTurnover> page = scoreTurnoverRepository.findAll(pageable);
        List<ScoreTurnoverDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public Optional<ScoreTurnoverDTO> findById(Long id) {
        return scoreTurnoverRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public List<ScoreTurnoverDTO> findByEmployeId(Long employeId) {
        return scoreTurnoverRepository.findByEmployeId(employeId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoreTurnoverDTO> findHistoriqueEmploye(Long employeId) {
        return scoreTurnoverRepository.findHistoriqueEmploye(employeId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ScoreTurnoverDTO> findDernierScoreEmploye(Long employeId) {
        return scoreTurnoverRepository.findDernierScoreEmploye(employeId)
                .stream().findFirst()
                .map(mapper::toDto);
    }

    @Override
    public List<ScoreTurnoverDTO> findByNiveauRisque(String niveauRisque) {
        return scoreTurnoverRepository.findByNiveauRisque(niveauRisque).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoreTurnoverDTO> findScoresRisques() {
        return scoreTurnoverRepository.findScoresRisques().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoreTurnoverDTO> findScoresCritiques() {
        return scoreTurnoverRepository.findScoresCritiques().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoreTurnoverDTO> findDerniersScores() {
        return scoreTurnoverRepository.findDerniersScores().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ==================== CALCUL DES SCORES (dynamique) ====================

    @Override
    @Transactional
    public ScoreTurnoverDTO calculerScorePourEmploye(Long employeId, Long systemeBIId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        SystemeBI systemeBI = systemeBIRepository.findById(systemeBIId)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", systemeBIId));

        // Récupération des poids (dynamiques)
        double poidsAnciennete = parametreService.getDouble("score.turnover.poids.anciennete", 0.25);
        double poidsSalaire     = parametreService.getDouble("score.turnover.poids.salaire", 0.30);
        double poidsPerformance = parametreService.getDouble("score.turnover.poids.performance", 0.20);
        double poidsFormation   = parametreService.getDouble("score.turnover.poids.formation", 0.15);
        double poidsAbsenteisme = parametreService.getDouble("score.turnover.poids.absenteisme", 0.10);

        double scoreAnciennete = calculerScoreAnciennete(employe.getDateEmbauche());
        double scoreSalaire    = calculerScoreSalaire(employe.getSalaire());
        double scoreFormation  = calculerScoreFormation(employe.getFormations().size());
        double scorePerformance = calculerScorePerformance(employeId);
        double scoreAbsenteisme = calculerScoreAbsenteisme(employeId);

        double scoreGlobal = scoreAnciennete * poidsAnciennete
                + scoreSalaire    * poidsSalaire
                + scorePerformance * poidsPerformance
                + scoreFormation   * poidsFormation
                + scoreAbsenteisme * poidsAbsenteisme;

        String niveau = determinerNiveauRisque(scoreGlobal);
        String facteurs = identifierFacteurs(scoreAnciennete, scoreSalaire, scorePerformance, scoreFormation, scoreAbsenteisme);
        String actions = recommanderActions(scoreAnciennete, scoreSalaire, scorePerformance, scoreFormation, scoreAbsenteisme, employe);

        ScoreTurnover score = ScoreTurnover.builder()
                .employe(employe)
                .systemeBI(systemeBI)
                .score(scoreGlobal)
                .niveauRisque(niveau)
                .datePrediction(LocalDate.now())
                .periodePrediction("6_MOIS")
                .facteursPrincipaux(facteurs)
                .actionRecommandee(actions)
                .confianceModele(parametreService.getDouble("score.turnover.confiance.modele", 0.85))
                .versionModele(parametreService.getValeur("score.turnover.version.modele", "v2.0"))
                .scoreAnciennete(scoreAnciennete)
                .scoreSalaire(scoreSalaire)
                .scorePerformance(scorePerformance)
                .scoreFormation(scoreFormation)
                .scoreAbsenteisme(scoreAbsenteisme)
                .build();

        ScoreTurnover saved = scoreTurnoverRepository.save(score);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<ScoreTurnoverDTO> calculerScoresPourTousEmployes(Long systemeBIId) {
        List<Employe> employes = employeRepository.findAll();
        List<ScoreTurnoverDTO> resultats = new ArrayList<>();
        for (Employe e : employes) {
            try {
                resultats.add(calculerScorePourEmploye(e.getId(), systemeBIId));
            } catch (Exception ex) {
                log.error("Erreur lors du calcul pour employé {}: {}", e.getId(), ex.getMessage());
            }
        }
        return resultats;
    }

    @Override
    public ScoreTurnoverDTO predireRisque(Long employeId, Long systemeBIId) {
        return calculerScorePourEmploye(employeId, systemeBIId);
    }

    // ==================== SUPPRESSION ====================

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

    // ==================== STATISTIQUES ====================

    @Override
    public Map<String, Long> getRepartitionRisques() {
        return scoreTurnoverRepository.repartitionRisquesActuels().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    public Double getScoreMoyenActuel() {
        return scoreTurnoverRepository.scoreMoyenActuel();
    }

    @Override
    public Map<String, Double> getScoreMoyenParDepartement() {
        return scoreTurnoverRepository.scoreMoyenParDepartement().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Double) arr[1]
                ));
    }

    @Override
    public List<ScoreTurnoverDTO> findScoresAvecActions() {
        return scoreTurnoverRepository.findAvecActionsRecommandees().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStatsTableauBord() {
        List<Object[]> stats = scoreTurnoverRepository.getStatsTableauBord();
        if (stats == null || stats.isEmpty()) {
            return Map.of(
                    "totalScores", 0L,
                    "scoreMoyen", 0.0,
                    "scoreMin", 0.0,
                    "scoreMax", 0.0,
                    "risqueCritique", 0L,
                    "risqueEleve", 0L,
                    "risqueMoyen", 0L,
                    "risqueFaible", 0L,
                    "confianceMoyenne", 0.0
            );
        }
        Object[] stat = stats.get(0);
        return Map.of(
                "totalScores", stat[0] != null ? stat[0] : 0L,
                "scoreMoyen", stat[1] != null ? stat[1] : 0.0,
                "scoreMin", stat[2] != null ? stat[2] : 0.0,
                "scoreMax", stat[3] != null ? stat[3] : 0.0,
                "risqueCritique", stat[4] != null ? stat[4] : 0L,
                "risqueEleve", stat[5] != null ? stat[5] : 0L,
                "risqueMoyen", stat[6] != null ? stat[6] : 0L,
                "risqueFaible", stat[7] != null ? stat[7] : 0L,
                "confianceMoyenne", stat[8] != null ? stat[8] : 0.0
        );
    }

    @Override
    public boolean hasScoreRecent(Long employeId, int jours) {
        LocalDate dateLimite = LocalDate.now().minusDays(jours);
        return scoreTurnoverRepository.hasScoreRecent(employeId, dateLimite);
    }

    // ==================== MÉTHODES PRIVÉES DYNAMIQUES ====================

    private double calculerScoreAnciennete(LocalDate dateEmbauche) {
        if (dateEmbauche == null) return 0;
        long annees = ChronoUnit.YEARS.between(dateEmbauche, LocalDate.now());
        int seuilAn1 = parametreService.getInt("score.anciennete.seuil.an1", 1);
        int seuilAn3 = parametreService.getInt("score.anciennete.seuil.an3", 3);
        int seuilAn5 = parametreService.getInt("score.anciennete.seuil.an5", 5);
        double valCourt   = parametreService.getDouble("score.anciennete.valeur.court", 30.0);
        double valMoyen   = parametreService.getDouble("score.anciennete.valeur.moyen", 15.0);
        double valLong    = parametreService.getDouble("score.anciennete.valeur.long", 5.0);
        double valTresLong = parametreService.getDouble("score.anciennete.valeur.treslong", 25.0);

        if (annees < seuilAn1) return valCourt;
        if (annees < seuilAn3) return valMoyen;
        if (annees < seuilAn5) return valLong;
        return valTresLong;
    }

    private double calculerScoreSalaire(Double salaire) {
        if (salaire == null) return 0;
        double seuilBas    = parametreService.getDouble("score.salaire.seuil.bas", 30000.0);
        double seuilMedium = parametreService.getDouble("score.salaire.seuil.medium", 45000.0);
        double seuilHaut   = parametreService.getDouble("score.salaire.seuil.haut", 60000.0);
        double valBas      = parametreService.getDouble("score.salaire.valeur.bas", 30.0);
        double valMedium   = parametreService.getDouble("score.salaire.valeur.medium", 15.0);
        double valHaut     = parametreService.getDouble("score.salaire.valeur.haut", 5.0);
        double valTresHaut = parametreService.getDouble("score.salaire.valeur.treshaut", 0.0);

        if (salaire < seuilBas) return valBas;
        if (salaire < seuilMedium) return valMedium;
        if (salaire < seuilHaut) return valHaut;
        return valTresHaut;
    }

    private double calculerScoreFormation(int nbFormations) {
        int seuilMax = parametreService.getInt("score.formation.seuil.max", 3);
        double valZero = parametreService.getDouble("score.formation.valeur.zero", 20.0);
        double valPeu  = parametreService.getDouble("score.formation.valeur.peu", 10.0);
        double valNorm = parametreService.getDouble("score.formation.valeur.normal", 0.0);

        if (nbFormations == 0) return valZero;
        if (nbFormations < seuilMax) return valPeu;
        return valNorm;
    }

    private double calculerScorePerformance(Long employeId) {
        double moyenneIdeale = parametreService.getDouble("score.performance.moyenne.ideal", 10.0);
        double coefficient   = parametreService.getDouble("score.performance.coefficient", 10.0);
        Double moyenne = evaluationRepository.moyenneEvaluationAnnuelle(employeId, LocalDate.now().getYear());
        if (moyenne == null || moyenne == 0) return coefficient; // fallback : score maximum
        return (moyenneIdeale - moyenne) * coefficient;
    }

    private double calculerScoreAbsenteisme(Long employeId) {
        Optional<IndicateurRH> dernierAbs = indicateurRHRepository
                .findTopByEmployeIdAndTypeOrderByDateCalculDesc(employeId, "ABSENTEISME");
        if (dernierAbs.isEmpty() || dernierAbs.get().getValeur() == null) {
            return 0.0;
        }
        double taux = dernierAbs.get().getValeur();
        double seuilEleve = parametreService.getDouble("score.absenteisme.seuil.eleve", 10.0);
        double seuilMoyen = parametreService.getDouble("score.absenteisme.seuil.moyen", 5.0);
        double valEleve   = parametreService.getDouble("score.absenteisme.valeur.eleve", 80.0);
        double valMoyen   = parametreService.getDouble("score.absenteisme.valeur.moyen", 40.0);
        double valFaible  = parametreService.getDouble("score.absenteisme.valeur.faible", 0.0);

        if (taux > seuilEleve) return valEleve;
        if (taux > seuilMoyen) return valMoyen;
        return valFaible;
    }

    private String determinerNiveauRisque(double score) {
        double seuilFaibleMoyen = parametreService.getDouble("score.turnover.seuil.faible.moyen", 20.0);
        double seuilMoyenEleve  = parametreService.getDouble("score.turnover.seuil.moyen.eleve", 40.0);
        double seuilEleveCritique = parametreService.getDouble("score.turnover.seuil.eleve.critique", 70.0);

        if (score < seuilFaibleMoyen) return "FAIBLE";
        if (score < seuilMoyenEleve) return "MOYEN";
        if (score < seuilEleveCritique) return "ELEVE";
        return "CRITIQUE";
    }

    private String identifierFacteurs(double anciennete, double salaire, double performance,
                                      double formation, double absenteisme) {
        double seuilAnciennete = parametreService.getDouble("facteur.anciennete.seuil", 20.0);
        double seuilSalaire    = parametreService.getDouble("facteur.salaire.seuil", 20.0);
        double seuilPerformance = parametreService.getDouble("facteur.performance.seuil", 20.0);
        double seuilFormation   = parametreService.getDouble("facteur.formation.seuil", 15.0);
        double seuilAbsenteisme = parametreService.getDouble("facteur.absenteisme.seuil", 15.0);
        StringBuilder sb = new StringBuilder();
        if (anciennete >= seuilAnciennete) sb.append("Ancienneté critique; ");
        if (salaire >= seuilSalaire) sb.append("Salaire bas; ");
        if (performance >= seuilPerformance) sb.append("Performance faible; ");
        if (formation >= seuilFormation) sb.append("Manque de formations; ");
        if (absenteisme >= seuilAbsenteisme) sb.append("Absentéisme élevé; ");
        return sb.length() > 0 ? sb.toString() : "Aucun facteur critique identifié";
    }

    private String recommanderActions(double anciennete, double salaire, double performance,
                                      double formation, double absenteisme, Employe employe) {
        double seuilSalaireAction = parametreService.getDouble("action.salaire.seuil", 20.0);
        double seuilFormationAction = parametreService.getDouble("action.formation.seuil", 15.0);
        double seuilAncienneteAction = parametreService.getDouble("action.anciennete.seuil", 20.0);
        int anneesAnciennetePourEvolution = parametreService.getInt("action.evolution.annees.min", 5);
        int anneesAnciennetePourOnboarding = parametreService.getInt("action.onboarding.annees.max", 1);
        double seuilAbsenteismeAction = parametreService.getDouble("action.absenteisme.seuil", 15.0);

        StringBuilder sb = new StringBuilder();
        if (salaire >= seuilSalaireAction) sb.append("- Revoir la rémunération\n");
        if (formation >= seuilFormationAction) sb.append("- Proposer des formations adaptées\n");
        if (anciennete >= seuilAncienneteAction && employe.getDateEmbauche() != null &&
                ChronoUnit.YEARS.between(employe.getDateEmbauche(), LocalDate.now()) > anneesAnciennetePourEvolution) {
            sb.append("- Discuter des opportunités d'évolution\n");
        }
        if (anciennete >= seuilAncienneteAction && employe.getDateEmbauche() != null &&
                ChronoUnit.YEARS.between(employe.getDateEmbauche(), LocalDate.now()) < anneesAnciennetePourOnboarding) {
            sb.append("- Renforcer l'onboarding et le suivi\n");
        }
        if (absenteisme >= seuilAbsenteismeAction) sb.append("- Entretien RH pour comprendre les causes d'absence\n");
        if (sb.length() == 0) sb.append("- Maintenir le suivi régulier");
        return sb.toString();
    }
}