package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.ScoreTurnover;
import com.codeWithProject.ecom.entity.SystemeBI;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ScoreTurnoverRepository;
import com.codeWithProject.ecom.repository.SystemeBIRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ScoreTurnoverServiceImpl implements ScoreTurnoverService {

    private final ScoreTurnoverRepository scoreTurnoverRepository;
    private final EmployeRepository employeRepository;
    private final SystemeBIRepository systemeBIRepository;
    private final ScoreTurnoverMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findAll() {
        log.debug("Récupération de tous les scores");
        return scoreTurnoverRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScoreTurnoverDTO> findAll(Pageable pageable) {
        log.debug("Récupération des scores avec pagination");
        Page<ScoreTurnover> page = scoreTurnoverRepository.findAll(pageable);
        List<ScoreTurnoverDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScoreTurnoverDTO> findById(Long id) {
        log.debug("Recherche de score par ID: {}", id);
        return scoreTurnoverRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findByEmployeId(Long employeId) {
        log.debug("Recherche des scores de l'employé ID: {}", employeId);
        return scoreTurnoverRepository.findByEmployeId(employeId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findHistoriqueEmploye(Long employeId) {
        log.debug("Recherche de l'historique des scores de l'employé ID: {}", employeId);
        return scoreTurnoverRepository.findHistoriqueEmploye(employeId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScoreTurnoverDTO> findDernierScoreEmploye(Long employeId) {
        log.debug("Recherche du dernier score de l'employé ID: {}", employeId);
        List<ScoreTurnover> scores = scoreTurnoverRepository.findDernierScoreEmploye(employeId);
        return scores.stream().findFirst().map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findByNiveauRisque(String niveauRisque) {
        log.debug("Recherche des scores de niveau: {}", niveauRisque);
        return scoreTurnoverRepository.findByNiveauRisque(niveauRisque).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findScoresRisques() {
        log.debug("Recherche des scores à risque (ELEVE/CRITIQUE)");
        return scoreTurnoverRepository.findScoresRisques().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findScoresCritiques() {
        log.debug("Recherche des scores critiques");
        return scoreTurnoverRepository.findScoresCritiques().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findDerniersScores() {
        log.debug("Recherche des derniers scores");
        return scoreTurnoverRepository.findDerniersScores().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ScoreTurnoverDTO calculerScorePourEmploye(Long employeId, Long systemeBIId) {
        log.debug("Calcul du score pour l'employé ID: {}", employeId);

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        SystemeBI systemeBI = systemeBIRepository.findById(systemeBIId)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", systemeBIId));

        ScoreTurnover score = ScoreTurnover.builder()
                .employe(employe)
                .systemeBI(systemeBI)
                .build();

        score.calculerScore();

        ScoreTurnover saved = scoreTurnoverRepository.save(score);
        log.info("Score calculé pour l'employé {} - Risque: {}", employeId, saved.getNiveauRisque());

        return mapper.toDto(saved);
    }

    @Override
    public List<ScoreTurnoverDTO> calculerScoresPourTousEmployes(Long systemeBIId) {
        log.debug("Calcul des scores pour tous les employés");

        List<Employe> employes = employeRepository.findAll();
        SystemeBI systemeBI = systemeBIRepository.findById(systemeBIId)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", systemeBIId));

        List<ScoreTurnover> scores = employes.stream()
                .map(employe -> {
                    ScoreTurnover score = ScoreTurnover.builder()
                            .employe(employe)
                            .systemeBI(systemeBI)
                            .build();
                    score.calculerScore();
                    return score;
                })
                .collect(Collectors.toList());

        List<ScoreTurnover> saved = scoreTurnoverRepository.saveAll(scores);
        log.info("Scores calculés pour {} employés", saved.size());

        return saved.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ScoreTurnoverDTO predireRisque(Long employeId, Long systemeBIId) {
        log.debug("Prédiction de risque pour l'employé ID: {}", employeId);

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        SystemeBI systemeBI = systemeBIRepository.findById(systemeBIId)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", systemeBIId));

        ScoreTurnover score = ScoreTurnover.builder()
                .employe(employe)
                .systemeBI(systemeBI)
                .build();

        score.predireRisque();

        ScoreTurnover saved = scoreTurnoverRepository.save(score);
        log.info("Risque prédit pour l'employé {} - Risque: {}", employeId, saved.getNiveauRisque());

        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression du score ID: {}", id);

        if (!scoreTurnoverRepository.existsById(id)) {
            throw new ResourceNotFoundException("ScoreTurnover", id);
        }

        scoreTurnoverRepository.deleteById(id);
        log.info("Score supprimé avec succès - ID: {}", id);
    }

    @Override
    public void deleteScoresObsoletes(int jours) {
        LocalDate dateLimite = LocalDate.now().minusDays(jours);
        log.debug("Suppression des scores antérieurs au {}", dateLimite);

        List<ScoreTurnover> obsoletes = scoreTurnoverRepository.findScoresObsoletes(dateLimite);
        scoreTurnoverRepository.deleteAll(obsoletes);
        log.info("{} scores obsolètes supprimés", obsoletes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getRepartitionRisques() {
        return scoreTurnoverRepository.repartitionRisquesActuels().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getScoreMoyenActuel() {
        return scoreTurnoverRepository.scoreMoyenActuel();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getScoreMoyenParDepartement() {
        return scoreTurnoverRepository.scoreMoyenParDepartement().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Double) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTurnoverDTO> findScoresAvecActions() {
        return scoreTurnoverRepository.findAvecActionsRecommandees().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public boolean hasScoreRecent(Long employeId, int jours) {
        LocalDate dateLimite = LocalDate.now().minusDays(jours);
        return scoreTurnoverRepository.hasScoreRecent(employeId, dateLimite);
    }
}