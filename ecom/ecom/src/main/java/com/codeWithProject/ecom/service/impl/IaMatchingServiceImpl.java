package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.service.dto.CvAnalyseResponseDTO;
import com.codeWithProject.ecom.service.dto.IaMatchingRequestDTO;
import com.codeWithProject.ecom.service.dto.IaMatchingResponseDTO;
import com.codeWithProject.ecom.entity.Candidature;
import com.codeWithProject.ecom.entity.CvAnalyse;
import com.codeWithProject.ecom.entity.OffreRecrutement;
import com.codeWithProject.ecom.entity.RecrutementScore;
import com.codeWithProject.ecom.entity.enums.NiveauCompatibilite;
import com.codeWithProject.ecom.entity.enums.StatutCandidature;
import com.codeWithProject.ecom.repository.CandidatureRepository;
import com.codeWithProject.ecom.repository.CvAnalyseRepository;
import com.codeWithProject.ecom.repository.RecrutementScoreRepository;
import com.codeWithProject.ecom.service.CvStorageService;
import com.codeWithProject.ecom.service.IaMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional
public class IaMatchingServiceImpl implements IaMatchingService {

    private final CvAnalyseRepository cvAnalyseRepository;
    private final RecrutementScoreRepository recrutementScoreRepository;
    private final CandidatureRepository candidatureRepository;
    private final CvStorageService cvStorageService;

   @Value("${ai.matching.url}")
private String aiMatchingUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public IaMatchingResponseDTO analyserCandidature(Candidature candidature) {
        if (candidature == null || candidature.getId() == null) {
            throw new RuntimeException("Candidature invalide.");
        }

        OffreRecrutement offre = candidature.getOffre();

        if (offre == null) {
            throw new RuntimeException("Offre introuvable pour cette candidature.");
        }

        Path cvPath = cvStorageService.getCvPath(candidature.getCvFileName());
        String cvText = cvStorageService.extractTextFromCv(cvPath, candidature.getCvContentType());

        IaMatchingRequestDTO request = IaMatchingRequestDTO.builder()
                .candidatureId(candidature.getId())
                .offreId(offre.getId())
                .employeId(candidature.getEmploye().getId())
                .titrePoste(offre.getTitrePoste())
                .description(offre.getDescription())
                .competencesRequises(offre.getCompetencesRequises())
                .technologiesRequises(offre.getTechnologiesRequises())
                .experienceMin(offre.getExperienceMin())
                .niveauEtude(offre.getNiveauEtude())
                .cvText(cvText)
                .build();

        IaMatchingResponseDTO iaResponse = callIaService(request);

        saveAnalyseAndScore(candidature, iaResponse, cvText);

        candidature.setStatut(StatutCandidature.ANALYSEE);
        candidatureRepository.save(candidature);

        return iaResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public CvAnalyseResponseDTO getAnalyseByCandidature(Long candidatureId) {
        CvAnalyse analyse = cvAnalyseRepository.findByCandidatureId(candidatureId)
                .orElseThrow(() -> new RuntimeException("Analyse CV introuvable pour candidature : " + candidatureId));

        return toCvAnalyseResponse(analyse);
    }

 private IaMatchingResponseDTO callIaService(IaMatchingRequestDTO request) {
    try {
        IaMatchingResponseDTO response = restTemplate.postForObject(
                aiMatchingUrl + "/api/matching/analyze",
                request,
                IaMatchingResponseDTO.class
        );

        if (response == null) {
            System.err.println("⚠️ Service IA sans réponse, utilisation fallback.");
            return fallbackMatching(request);
        }

        return response;

    } catch (Exception e) {
        System.err.println("⚠️ Service IA indisponible, utilisation fallback : " + e.getMessage());
        return fallbackMatching(request);
    }
}

    private IaMatchingResponseDTO fallbackMatching(IaMatchingRequestDTO request) {
        List<String> competencesRequises = safeList(request.getCompetencesRequises());
        List<String> technologiesRequises = safeList(request.getTechnologiesRequises());

        List<String> competencesCorrespondantes = new ArrayList<>();
        List<String> competencesManquantes = new ArrayList<>();

        List<String> technologiesCorrespondantes = new ArrayList<>();
        List<String> technologiesManquantes = new ArrayList<>();

        /*
         * Fallback provisoire :
         * comme on n'a pas encore une vraie extraction IA,
         * on considère une partie des critères comme détectés.
         * Ce bloc sera remplacé par le microservice IA réel.
         */

        for (int i = 0; i < competencesRequises.size(); i++) {
            if (i % 2 == 0) {
                competencesCorrespondantes.add(competencesRequises.get(i));
            } else {
                competencesManquantes.add(competencesRequises.get(i));
            }
        }

        for (int i = 0; i < technologiesRequises.size(); i++) {
            if (i % 2 == 0) {
                technologiesCorrespondantes.add(technologiesRequises.get(i));
            } else {
                technologiesManquantes.add(technologiesRequises.get(i));
            }
        }

        int totalCriteria = competencesRequises.size() + technologiesRequises.size();
        int matchedCriteria = competencesCorrespondantes.size() + technologiesCorrespondantes.size();

        int scoreGlobal = totalCriteria == 0
                ? 0
                : Math.round((matchedCriteria * 100f) / totalCriteria);

        int scoreCompetences = competencesRequises.isEmpty()
                ? 0
                : Math.round((competencesCorrespondantes.size() * 100f) / competencesRequises.size());

        int scoreTechnologies = technologiesRequises.isEmpty()
                ? 0
                : Math.round((technologiesCorrespondantes.size() * 100f) / technologiesRequises.size());

        NiveauCompatibilite niveau = resolveNiveau(scoreGlobal);

        return IaMatchingResponseDTO.builder()
                .texteExtrait(request.getCvText())
                .competencesDetectees(competencesCorrespondantes)
                .technologiesDetectees(technologiesCorrespondantes)
                .experiencesDetectees(new ArrayList<>())
                .anneesExperienceEstimees(0)
                .resumeProfil("Analyse provisoire générée par le fallback backend. Le microservice IA réel sera connecté ensuite.")
                .pointsForts(competencesCorrespondantes)
                .pointsFaibles(competencesManquantes)
                .scoreGlobal(scoreGlobal)
                .scoreCompetences(scoreCompetences)
                .scoreTechnologies(scoreTechnologies)
                .scoreExperience(0)
                .scoreFormation(0)
                .niveauCompatibilite(niveau)
                .competencesCorrespondantes(competencesCorrespondantes)
                .competencesManquantes(competencesManquantes)
                .technologiesCorrespondantes(technologiesCorrespondantes)
                .technologiesManquantes(technologiesManquantes)
                .justificationIa("Score provisoire calculé selon les critères de l'offre. Le vrai scoring IA sera branché via le microservice Python.")
                .recommandationIa(resolveRecommandation(scoreGlobal))
                .build();
    }

    private void saveAnalyseAndScore(
            Candidature candidature,
            IaMatchingResponseDTO response,
            String cvText
    ) {
        CvAnalyse analyse = cvAnalyseRepository.findByCandidatureId(candidature.getId())
                .orElseGet(CvAnalyse::new);

        analyse.setCandidature(candidature);
        analyse.setOffre(candidature.getOffre());
        analyse.setEmploye(candidature.getEmploye());
        analyse.setCvFileName(candidature.getCvFileName());
        analyse.setCvContentType(candidature.getCvContentType());
        analyse.setTexteExtrait(response.getTexteExtrait() != null ? response.getTexteExtrait() : cvText);
        analyse.setCompetencesDetectees(safeList(response.getCompetencesDetectees()));
        analyse.setTechnologiesDetectees(safeList(response.getTechnologiesDetectees()));
        analyse.setExperiencesDetectees(safeList(response.getExperiencesDetectees()));
        analyse.setAnneesExperienceEstimees(response.getAnneesExperienceEstimees());
        analyse.setResumeProfil(response.getResumeProfil());
        analyse.setPointsForts(safeList(response.getPointsForts()));
        analyse.setPointsFaibles(safeList(response.getPointsFaibles()));

        CvAnalyse savedAnalyse = cvAnalyseRepository.save(analyse);

        RecrutementScore score = recrutementScoreRepository.findByCandidatureId(candidature.getId())
                .orElseGet(RecrutementScore::new);

        score.setCandidature(candidature);
        score.setOffre(candidature.getOffre());
        score.setEmploye(candidature.getEmploye());
        score.setScoreGlobal(response.getScoreGlobal());
        score.setScoreCompetences(response.getScoreCompetences());
        score.setScoreTechnologies(response.getScoreTechnologies());
        score.setScoreExperience(response.getScoreExperience());
        score.setScoreFormation(response.getScoreFormation());
        score.setNiveauCompatibilite(response.getNiveauCompatibilite());
        score.setCompetencesCorrespondantes(safeList(response.getCompetencesCorrespondantes()));
        score.setCompetencesManquantes(safeList(response.getCompetencesManquantes()));
        score.setTechnologiesCorrespondantes(safeList(response.getTechnologiesCorrespondantes()));
        score.setTechnologiesManquantes(safeList(response.getTechnologiesManquantes()));
        score.setJustificationIa(response.getJustificationIa());
        score.setRecommandationIa(response.getRecommandationIa());

        RecrutementScore savedScore = recrutementScoreRepository.save(score);

        candidature.setAnalyseCv(savedAnalyse);
        candidature.setScore(savedScore);
    }

    private CvAnalyseResponseDTO toCvAnalyseResponse(CvAnalyse analyse) {
        return CvAnalyseResponseDTO.builder()
                .id(analyse.getId())
                .candidatureId(analyse.getCandidature() != null ? analyse.getCandidature().getId() : null)
                .offreId(analyse.getOffre() != null ? analyse.getOffre().getId() : null)
                .employeId(analyse.getEmploye() != null ? analyse.getEmploye().getId() : null)
                .cvFileName(analyse.getCvFileName())
                .cvContentType(analyse.getCvContentType())
                .texteExtrait(analyse.getTexteExtrait())
                .competencesDetectees(safeList(analyse.getCompetencesDetectees()))
                .technologiesDetectees(safeList(analyse.getTechnologiesDetectees()))
                .experiencesDetectees(safeList(analyse.getExperiencesDetectees()))
                .anneesExperienceEstimees(analyse.getAnneesExperienceEstimees())
                .resumeProfil(analyse.getResumeProfil())
                .pointsForts(safeList(analyse.getPointsForts()))
                .pointsFaibles(safeList(analyse.getPointsFaibles()))
                .dateAnalyse(analyse.getDateAnalyse())
                .build();
    }

    private NiveauCompatibilite resolveNiveau(Integer score) {
        int value = score == null ? 0 : score;

        if (value >= 85) return NiveauCompatibilite.EXCELLENT;
        if (value >= 70) return NiveauCompatibilite.TRES_BON;
        if (value >= 55) return NiveauCompatibilite.BON;
        if (value >= 40) return NiveauCompatibilite.MOYEN;

        return NiveauCompatibilite.FAIBLE;
    }

    private String resolveRecommandation(Integer score) {
        int value = score == null ? 0 : score;

        if (value >= 85) {
            return "Profil fortement recommandé pour ce poste.";
        }

        if (value >= 70) {
            return "Profil intéressant, à considérer pour un entretien.";
        }

        if (value >= 55) {
            return "Profil acceptable, mais certaines compétences doivent être validées.";
        }

        if (value >= 40) {
            return "Profil partiellement compatible.";
        }

        return "Profil peu compatible avec les critères actuels.";
    }

   private List<String> safeList(List<String> values) {
    if (values == null) {
        return new ArrayList<>();
    }

    return values.stream()
            .filter(value -> value != null && !value.trim().isEmpty())
            .map(String::trim)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
}
}