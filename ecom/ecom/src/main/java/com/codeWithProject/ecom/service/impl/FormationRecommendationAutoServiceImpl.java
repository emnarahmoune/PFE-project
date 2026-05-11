package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.entity.FormationRecommendation;
import com.codeWithProject.ecom.entity.PosteCompetence;
import com.codeWithProject.ecom.repository.EmployeCompetenceRepository;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.FormationRecommendationRepository;
import com.codeWithProject.ecom.repository.FormationRepository;
import com.codeWithProject.ecom.repository.PosteCompetenceRepository;
import com.codeWithProject.ecom.service.FormationAiRecommendationService;
import com.codeWithProject.ecom.service.FormationRecommendationAutoService;
import com.codeWithProject.ecom.service.dto.AiFormationDto;
import com.codeWithProject.ecom.service.dto.FormationRecommendationRequest;
import com.codeWithProject.ecom.service.dto.FormationRecommendationResponse;
import com.codeWithProject.ecom.service.dto.RecommendationItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationRecommendationAutoServiceImpl implements FormationRecommendationAutoService {

    private final FormationAiRecommendationService formationAiRecommendationService;
    private final FormationRecommendationRepository formationRecommendationRepository;

    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final PosteCompetenceRepository posteCompetenceRepository;
    private final FormationRepository formationRepository;

    private final ObjectMapper objectMapper;

    @Override
    public void generateAllRecommendationsForEmploye(Employe employe) {
        if (employe == null || employe.getId() == null) {
            return;
        }

        generateBoostRecommendationsForEmploye(employe);
        generateGapRecommendationsForEmployePoste(employe);
    }

    @Override
    public void generateBoostRecommendationsForEmploye(Employe employe) {
        if (employe == null || employe.getId() == null) {
            return;
        }

        var userSkills = getUserSkills(employe.getId());

        if (userSkills.isEmpty()) {
            return;
        }

        List<String> formationsSuivies = getFormationsSuivies(employe.getId());
        List<AiFormationDto> formations = getActiveFormationsForAi();

        FormationRecommendationRequest request = FormationRecommendationRequest.builder()
                .mode("BOOST_COMPETENCES")
                .poste(employe.getPoste())
                .userSkills(userSkills)
                .requiredSkills(Collections.emptyMap())
                .formations(formations)
                .formationsSuivies(formationsSuivies)
                .build();

        FormationRecommendationResponse response = formationAiRecommendationService.recommend(request);

        formationRecommendationRepository.deleteByEmployeIdAndOffreIdIsNullAndType(
                employe.getId(),
                "BOOST_COMPETENCES"
        );

        saveRecommendations(employe, null, response);
    }

    @Override
    public void generateGapRecommendationsForEmployePoste(Employe employe) {
        if (employe == null || employe.getId() == null) {
            return;
        }

        if (employe.getPoste() == null || employe.getPoste().isBlank()) {
            return;
        }

        var userSkills = getUserSkills(employe.getId());
        var requiredSkills = getRequiredSkillsFromPoste(employe.getPoste());

        if (userSkills.isEmpty()) {
            return;
        }

        List<String> formationsSuivies = getFormationsSuivies(employe.getId());
        List<AiFormationDto> formations = getActiveFormationsForAi();

        FormationRecommendationRequest request = FormationRecommendationRequest.builder()
                .mode("GAP_POSTE")
                .poste(employe.getPoste())
                .userSkills(userSkills)
                .requiredSkills(requiredSkills)
                .formations(formations)
                .formationsSuivies(formationsSuivies)
                .build();

        FormationRecommendationResponse response = formationAiRecommendationService.recommend(request);

        formationRecommendationRepository.deleteByEmployeIdAndOffreIdIsNullAndType(
                employe.getId(),
                "GAP_POSTE"
        );

        saveRecommendations(employe, null, response);
    }

    private List<AiFormationDto> getActiveFormationsForAi() {
        return formationRepository.findActiveWithDetails()
                .stream()
                .map(formation -> AiFormationDto.builder()
                        .id(formation.getId())
                        .titre(formation.getTitre())
                        .description(formation.getDescription())
                        .domaine(formation.getDomaine())
                        .niveau(null)
                        .dureeHeures(formation.getDureeHeures())
                        .competences(Collections.emptyList())
                        .build()
                )
                .toList();
    }

    private java.util.Map<String, Integer> getUserSkills(Long employeId) {
        return employeCompetenceRepository.findByEmployeId(employeId)
                .stream()
                .filter(ec -> ec.getCompetence() != null && ec.getCompetence().getNom() != null)
                .collect(Collectors.toMap(
                        ec -> ec.getCompetence().getNom(),
                        ec -> niveauToInt(ec.getNiveau()),
                        Math::max,
                        LinkedHashMap::new
                ));
    }

    private java.util.Map<String, Integer> getRequiredSkillsFromPoste(String poste) {
        if (poste == null || poste.isBlank()) {
            return Collections.emptyMap();
        }

        List<PosteCompetence> posteCompetences =
                posteCompetenceRepository.findByPosteIgnoreCaseWithCompetence(poste);

        if (posteCompetences.isEmpty()) {
            posteCompetences =
                    posteCompetenceRepository.findMatchingPosteWithCompetence(poste);
        }

        return posteCompetences
                .stream()
                .filter(pc -> pc.getCompetence() != null && pc.getCompetence().getNom() != null)
                .collect(Collectors.toMap(
                        pc -> pc.getCompetence().getNom(),
                        pc -> pc.getNiveauRequis() == null ? 3 : pc.getNiveauRequis(),
                        Math::max,
                        LinkedHashMap::new
                ));
    }

    private List<String> getFormationsSuivies(Long employeId) {
        return employeFormationRepository.findByEmployeId(employeId)
                .stream()
                .filter(ef -> ef.getFormation() != null)
                .map(EmployeFormation::getFormation)
                .filter(formation -> formation.getTitre() != null && !formation.getTitre().isBlank())
                .map(Formation::getTitre)
                .toList();
    }

    private Integer niveauToInt(String niveau) {
        if (niveau == null) {
            return 0;
        }

        String n = niveau.trim().toUpperCase();

        return switch (n) {
            case "DEBUTANT", "DÉBUTANT", "BEGINNER" -> 1;
            case "INTERMEDIAIRE", "INTERMÉDIAIRE", "MOYEN", "JUNIOR" -> 2;
            case "AVANCE", "AVANCÉ", "SENIOR" -> 3;
            case "EXPERT" -> 4;
            default -> {
                try {
                    yield Integer.parseInt(niveau);
                } catch (Exception e) {
                    yield 0;
                }
            }
        };
    }

    private void saveRecommendations(
            Employe employe,
            Long offreId,
            FormationRecommendationResponse response
    ) {
        if (response == null || response.getRecommendations() == null) {
            return;
        }

        for (RecommendationItem item : response.getRecommendations()) {
            if (item.getFormationId() == null) {
                continue;
            }

            String matchedSkills = item.getMatchedSkills() == null
                    ? ""
                    : String.join(", ", item.getMatchedSkills());

            String videosJson = "[]";

            try {
                if (item.getVideos() != null) {
                    videosJson = objectMapper.writeValueAsString(item.getVideos());
                }
            } catch (Exception e) {
                videosJson = "[]";
            }

            FormationRecommendation recommendation = FormationRecommendation.builder()
                    .employeId(employe.getId())
                    .offreId(offreId)
                    .formationId(item.getFormationId())
                    .poste(response.getPoste() != null ? response.getPoste() : employe.getPoste())
                    .type(item.getType())
                    .formationTitle(item.getFormation() != null ? item.getFormation() : item.getTitle())
                    .provider(item.getProvider())
                    .url(item.getUrl())
                    .description(item.getDescription())
                    .score(item.getScore())
                    .semanticScore(item.getSemanticScore())
                    .skillScore(item.getSkillScore())
                    .matchedSkills(matchedSkills)
                    .reason(item.getReason())
                    .videosJson(videosJson)
                    .dateCreation(LocalDateTime.now())
                    .build();

            formationRecommendationRepository.save(recommendation);
        }
    }
}