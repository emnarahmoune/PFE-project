package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Candidature;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.OffreRecrutement;
import com.codeWithProject.ecom.entity.RecrutementScore;
import com.codeWithProject.ecom.entity.enums.StatutCandidature;
import com.codeWithProject.ecom.entity.enums.StatutOffreRecrutement;
import com.codeWithProject.ecom.repository.CandidatureRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.OffreRecrutementRepository;
import com.codeWithProject.ecom.service.CandidatureService;
import com.codeWithProject.ecom.service.CvStorageService;
import com.codeWithProject.ecom.service.IaMatchingService;
import com.codeWithProject.ecom.service.NotificationService;
import com.codeWithProject.ecom.service.dto.CandidatureResponseDTO;
import com.codeWithProject.ecom.service.dto.CvAnalyseResponseDTO;
import com.codeWithProject.ecom.service.dto.DecisionCandidatureRequestDTO;
import com.codeWithProject.ecom.service.dto.OffreRecrutementResponse;
import com.codeWithProject.ecom.service.dto.RecrutementScoreResponse;
import com.codeWithProject.ecom.service.dto.TopCandidatureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidatureServiceImpl implements CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final OffreRecrutementRepository offreRecrutementRepository;
    private final EmployeRepository employeRepository;
    private final CvStorageService cvStorageService;
    private final IaMatchingService iaMatchingService;
    private final NotificationService notificationService;

    @Override
    public CandidatureResponseDTO postuler(
            Long offreId,
            Long employeId,
            String motivation,
            MultipartFile cv
    ) {
        OffreRecrutement offre = offreRecrutementRepository.findById(offreId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable avec id : " + offreId));

        if (offre.getStatut() != StatutOffreRecrutement.OUVERTE) {
            throw new RuntimeException("Cette offre n'est pas ouverte aux candidatures.");
        }

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec id : " + employeId));

        if (candidatureRepository.existsByOffreAndEmploye(offre, employe)) {
            throw new RuntimeException("Vous avez déjà postulé à cette offre.");
        }

        String storedFileName = cvStorageService.saveCv(cv);

        Candidature candidature = Candidature.builder()
                .offre(offre)
                .employe(employe)
                .motivation(motivation)
                .cvFileName(storedFileName)
                .cvOriginalName(cv.getOriginalFilename())
                .cvContentType(cv.getContentType())
                .cvPath(cvStorageService.getCvPath(storedFileName).toString())
                .statut(StatutCandidature.EN_ANALYSE)
                .dateSoumission(LocalDateTime.now())
                .build();

        Candidature saved = candidatureRepository.save(candidature);

        try {
            iaMatchingService.analyserCandidature(saved);
        } catch (Exception e) {
            System.err.println("Erreur analyse IA candidature " + saved.getId() + " : " + e.getMessage());
            saved.setStatut(StatutCandidature.SOUMISE);
        }

        return toCandidatureResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getMesCandidatures(Long employeId) {
        return candidatureRepository.findWithOffreAndEmployeAndScoreByEmployeIdOrderByDateSoumissionDesc(employeId)
                .stream()
                .map(this::toCandidatureResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAlreadyApplied(Long offreId, Long employeId) {
        return candidatureRepository.existsByOffreIdAndEmployeId(offreId, employeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getCandidaturesByOffre(Long offreId) {
        return candidatureRepository.findWithOffreAndEmployeAndScoreByOffreIdOrderByDateSoumissionDesc(offreId)
                .stream()
                .map(this::toCandidatureResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopCandidatureResponse> getTopCandidatures(Long offreId, int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;

        return candidatureRepository.findTopCandidaturesByOffreId(offreId, PageRequest.of(0, safeLimit))
                .stream()
                .map(candidature -> TopCandidatureResponse.builder()
                        .candidature(toCandidatureResponse(candidature))
                        .scoreGlobal(
                                candidature.getScore() != null && candidature.getScore().getScoreGlobal() != null
                                        ? candidature.getScore().getScoreGlobal()
                                        : 0
                        )
                        .niveauCompatibilite(
                                candidature.getScore() != null
                                        && candidature.getScore().getNiveauCompatibilite() != null
                                        ? candidature.getScore().getNiveauCompatibilite().name()
                                        : null
                        )
                        .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public CandidatureResponseDTO accepterCandidature(
            Long candidatureId,
            DecisionCandidatureRequestDTO request
    ) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable avec id : " + candidatureId));

        OffreRecrutement offre = candidature.getOffre();

        if (offre == null) {
            throw new RuntimeException("Offre liée à la candidature introuvable.");
        }

        if (candidature.getStatut() == StatutCandidature.REFUSEE) {
            throw new RuntimeException("Cette candidature est déjà refusée.");
        }

        if (candidature.getStatut() == StatutCandidature.ACCEPTEE) {
            throw new RuntimeException("Cette candidature est déjà acceptée.");
        }

        boolean dejaAcceptee = candidatureRepository
                .findByOffreIdOrderByDateSoumissionDesc(offre.getId())
                .stream()
                .anyMatch(c ->
                        c.getStatut() == StatutCandidature.ACCEPTEE
                                && !c.getId().equals(candidatureId)
                );

        if (dejaAcceptee) {
            throw new RuntimeException("Une candidature est déjà acceptée pour cette offre.");
        }

        candidature.setStatut(StatutCandidature.ACCEPTEE);
        candidature.setDateDecision(LocalDateTime.now());
        candidature.setDecisionCommentaire(request != null ? request.getCommentaire() : null);

        Employe employe = candidature.getEmploye();

        if (employe != null) {
            employe.setPoste(offre.getTitrePoste());

            if (offre.getDepartement() != null && !offre.getDepartement().isBlank()) {
                employe.setDepartement(offre.getDepartement());
            }

            if (offre.getSalairePropose() != null) {
                employe.setSalaire(offre.getSalairePropose());
            }

            notificationService.createNotification(
                    employe.getId(),
                    "Félicitations ! Votre candidature pour le poste "
                            + offre.getTitrePoste()
                            + " a été acceptée. Votre poste, votre département"
                            + " et votre salaire ont été mis à jour.",
                    "RECRUTEMENT",
                    candidature.getId()
            );
        }

        List<Candidature> autresCandidatures =
                candidatureRepository.findByOffreIdOrderByDateSoumissionDesc(offre.getId());

        for (Candidature autre : autresCandidatures) {
            if (!autre.getId().equals(candidatureId)
                    && autre.getStatut() != StatutCandidature.REFUSEE) {
                autre.setStatut(StatutCandidature.REFUSEE);
                autre.setDateDecision(LocalDateTime.now());
                autre.setDecisionCommentaire(
                        "Refus automatique : une autre candidature a été acceptée pour cette offre."
                );
            }
        }

        offre.setStatut(StatutOffreRecrutement.FERMEE);

        return toCandidatureResponse(candidature);
    }

    @Override
    public CandidatureResponseDTO refuserCandidature(
            Long candidatureId,
            DecisionCandidatureRequestDTO request
    ) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable avec id : " + candidatureId));

        if (candidature.getStatut() == StatutCandidature.ACCEPTEE) {
            throw new RuntimeException("Impossible de refuser une candidature déjà acceptée.");
        }

        if (candidature.getStatut() == StatutCandidature.REFUSEE) {
            throw new RuntimeException("Cette candidature est déjà refusée.");
        }

        candidature.setStatut(StatutCandidature.REFUSEE);
        candidature.setDateDecision(LocalDateTime.now());
        candidature.setDecisionCommentaire(request != null ? request.getCommentaire() : null);

        return toCandidatureResponse(candidature);
    }

    @Override
    public CandidatureResponseDTO relancerAnalyseIa(Long candidatureId) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RuntimeException("Candidature introuvable avec id : " + candidatureId));

        if (candidature.getStatut() == StatutCandidature.ACCEPTEE
                || candidature.getStatut() == StatutCandidature.REFUSEE) {
            throw new RuntimeException("Impossible de relancer l'analyse IA sur une candidature finalisée.");
        }

        candidature.setStatut(StatutCandidature.EN_ANALYSE);

        try {
            iaMatchingService.analyserCandidature(candidature);
        } catch (Exception e) {
            System.err.println("Erreur relance analyse IA candidature " + candidature.getId() + " : " + e.getMessage());
            candidature.setStatut(StatutCandidature.SOUMISE);

            throw new RuntimeException(
                    "Impossible de lancer l'analyse IA. Vérifiez que le microservice Python est démarré sur le port 8001."
            );
        }

        return toCandidatureResponse(candidature);
    }

    private CandidatureResponseDTO toCandidatureResponse(Candidature candidature) {
        if (candidature == null) {
            return null;
        }

        Employe employe = candidature.getEmploye();

        return CandidatureResponseDTO.builder()
                .id(candidature.getId())
                .offreId(candidature.getOffre() != null ? candidature.getOffre().getId() : null)
                .employeId(employe != null ? employe.getId() : null)
                .employeNom(employe != null ? employe.getNom() : null)
                .employePrenom(employe != null ? employe.getPrenom() : null)
                .employeEmail(employe != null ? employe.getEmail() : null)
                .employePosteActuel(employe != null ? employe.getPoste() : null)
                .employeDepartement(employe != null ? employe.getDepartement() : null)
                .employePhotoUrl(employe != null ? getBestEmployePhotoUrl(employe) : null)
                .motivation(candidature.getMotivation())
                .cvFileName(candidature.getCvFileName())
                .cvUrl(
                        candidature.getCvFileName() != null
                                ? "/api/candidatures/" + candidature.getId() + "/cv"
                                : null
                )
                .statut(candidature.getStatut())
                .dateSoumission(candidature.getDateSoumission())
                .dateDecision(candidature.getDateDecision())
                .decisionCommentaire(candidature.getDecisionCommentaire())
                .offre(toOffreResponse(candidature.getOffre()))
                .analyseCv(toCvAnalyseResponse(candidature))
                .score(toScoreResponse(candidature.getScore()))
                .build();
    }

    private OffreRecrutementResponse toOffreResponse(OffreRecrutement offre) {
        if (offre == null) {
            return null;
        }

        Integer meilleurScore = offre.getCandidatures() == null
                ? null
                : offre.getCandidatures()
                .stream()
                .map(Candidature::getScore)
                .filter(score -> score != null && score.getScoreGlobal() != null)
                .map(RecrutementScore::getScoreGlobal)
                .max(Integer::compareTo)
                .orElse(null);

        return OffreRecrutementResponse.builder()
                .id(offre.getId())
                .titrePoste(offre.getTitrePoste())
                .description(offre.getDescription())
                .departement(offre.getDepartement())
                .typeContrat(offre.getTypeContrat())
                .localisation(offre.getLocalisation())
                .competencesRequises(safeList(offre.getCompetencesRequises()))
                .technologiesRequises(safeList(offre.getTechnologiesRequises()))
                .experienceMin(offre.getExperienceMin())
                .niveauEtude(offre.getNiveauEtude())
                .statut(offre.getStatut())
                .datePublication(offre.getDatePublication())
                .dateExpiration(offre.getDateExpiration())
                .salairePropose(offre.getSalairePropose())
                .creeParId(offre.getCreeParId())
                .creeParNom(offre.getCreeParNom())
                .nombreCandidatures(
                        offre.getCandidatures() != null
                                ? offre.getCandidatures().size()
                                : 0
                )
                .meilleurScore(meilleurScore)
                .build();
    }

    private CvAnalyseResponseDTO toCvAnalyseResponse(Candidature candidature) {
        if (candidature == null || candidature.getAnalyseCv() == null) {
            return null;
        }

        var analyse = candidature.getAnalyseCv();

        return CvAnalyseResponseDTO.builder()
                .id(analyse.getId())
                .candidatureId(candidature.getId())
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

    private RecrutementScoreResponse toScoreResponse(RecrutementScore score) {
        if (score == null) {
            return null;
        }

        return RecrutementScoreResponse.builder()
                .id(score.getId())
                .candidatureId(score.getCandidature() != null ? score.getCandidature().getId() : null)
                .offreId(score.getOffre() != null ? score.getOffre().getId() : null)
                .employeId(score.getEmploye() != null ? score.getEmploye().getId() : null)
                .scoreGlobal(score.getScoreGlobal())
                .scoreCompetences(score.getScoreCompetences())
                .scoreTechnologies(score.getScoreTechnologies())
                .scoreExperience(score.getScoreExperience())
                .scoreFormation(score.getScoreFormation())
                .niveauCompatibilite(score.getNiveauCompatibilite())
                .competencesCorrespondantes(safeList(score.getCompetencesCorrespondantes()))
                .competencesManquantes(safeList(score.getCompetencesManquantes()))
                .technologiesCorrespondantes(safeList(score.getTechnologiesCorrespondantes()))
                .technologiesManquantes(safeList(score.getTechnologiesManquantes()))
                .justificationIa(score.getJustificationIa())
                .recommandationIa(score.getRecommandationIa())
                .dateCalcul(score.getDateCalcul())
                .build();
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


 private String getBestEmployePhotoUrl(Employe employe) {
    if (employe == null) {
        return null;
    }

    return employe.getPhotoUrl();
}
}