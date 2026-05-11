package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.entity.FormationRecommendation;
import com.codeWithProject.ecom.entity.FormationVideo;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.FormationRecommendationRepository;
import com.codeWithProject.ecom.repository.FormationRepository;
import com.codeWithProject.ecom.repository.FormationVideoRepository;
import com.codeWithProject.ecom.service.FormationRecommendationAutoService;
import com.codeWithProject.ecom.service.dto.RecommendedVideoDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/formations/recommendations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FormationRecommendationController {

    private final FormationRecommendationRepository formationRecommendationRepository;
    private final EmployeRepository employeRepository;
    private final FormationRecommendationAutoService formationRecommendationAutoService;
    private final FormationRepository formationRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final FormationVideoRepository formationVideoRepository;
    private final ObjectMapper objectMapper;

    // =========================================================
    // ANCIENS ENDPOINTS PAR ID EMPLOYÉ
    // =========================================================

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<FormationRecommendation>> getRecommendationsByEmploye(
            @PathVariable Long employeId
    ) {
        return ResponseEntity.ok(
                formationRecommendationRepository.findByEmployeIdOrderByScoreDesc(employeId)
        );
    }

    @GetMapping("/employe/{employeId}/auto")
    public ResponseEntity<List<FormationRecommendation>> generateAndGetRecommendationsByEmploye(
            @PathVariable Long employeId
    ) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec id : " + employeId));

        formationRecommendationAutoService.generateAllRecommendationsForEmploye(employe);

        return ResponseEntity.ok(
                formationRecommendationRepository.findByEmployeIdOrderByScoreDesc(employeId)
        );
    }

    // =========================================================
    // ENDPOINTS UTILISATEUR CONNECTÉ
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<List<FormationRecommendation>> getMyRecommendations(
            Authentication auth
    ) {
        Employe employe = getCurrentEmploye(auth);

        return ResponseEntity.ok(
                formationRecommendationRepository.findByEmployeIdOrderByScoreDesc(employe.getId())
        );
    }

    @GetMapping("/me/auto")
    public ResponseEntity<List<FormationRecommendation>> generateAndGetMyRecommendations(
            Authentication auth
    ) {
        Employe employe = getCurrentEmploye(auth);

        System.out.println("===== IA CONNECTED USER =====");
        System.out.println("EMPLOYE ID = " + employe.getId());
        System.out.println("EMPLOYE EMAIL = " + employe.getEmail());
        System.out.println("EMPLOYE POSTE = " + employe.getPoste());

        formationRecommendationAutoService.generateAllRecommendationsForEmploye(employe);

        return ResponseEntity.ok(
                formationRecommendationRepository.findByEmployeIdOrderByScoreDesc(employe.getId())
        );
    }

    // =========================================================
    // INSCRIPTION DEPUIS RECOMMANDATION IA
    // =========================================================

    @PostMapping("/{recommendationId}/inscrire")
    @Transactional
    public ResponseEntity<?> inscrireDepuisRecommendation(
            @PathVariable Long recommendationId,
            Authentication auth
    ) {
        FormationRecommendation recommendation = formationRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommandation introuvable id=" + recommendationId));

        Employe employe = getCurrentEmploye(auth);

        if (recommendation.getFormationId() == null) {
            throw new RuntimeException("Cette recommandation ne contient pas formationId. Régénère les recommandations IA.");
        }

        Formation formation = formationRepository.findById(recommendation.getFormationId())
                .orElseThrow(() -> new RuntimeException("Formation introuvable id=" + recommendation.getFormationId()));

        // 1. Corriger les vidéos de la formation avant inscription
        ensureFormationHasEmbeddableVideos(formation, recommendation);

        // 2. Inscrire l'employé à la formation existante
        boolean alreadyExists = employeFormationRepository.existsByEmployeIdAndFormationId(
                employe.getId(),
                formation.getId()
        );

        if (!alreadyExists) {
            EmployeFormation employeFormation = new EmployeFormation();
            employeFormation.setEmploye(employe);
            employeFormation.setFormation(formation);
            employeFormation.setStatut("EN_COURS");
            employeFormation.setProgression(0);
            employeFormation.setDateInscription(LocalDateTime.now());

            employeFormationRepository.save(employeFormation);
        }

        // 3. Supprimer uniquement cette recommandation
        formationRecommendationRepository.deleteById(recommendationId);

        return ResponseEntity.ok(Map.of(
                "message", alreadyExists
                        ? "Formation déjà présente dans vos formations"
                        : "Formation ajoutée à vos formations",
                "formationId", formation.getId(),
                "employeId", employe.getId()
        ));
    }

    // =========================================================
    // VIDÉOS IA : SUPPRIMER LES MAUVAISES ET CRÉER DES IFRAME
    // =========================================================

    private void ensureFormationHasEmbeddableVideos(
            Formation formation,
            FormationRecommendation recommendation
    ) {
        Long formationId = formation.getId();

        boolean hasBadSearchVideos =
                formationVideoRepository.existsByFormation_IdAndUrlYoutubeContainingIgnoreCase(
                        formationId,
                        "youtube.com/results"
                );

        if (hasBadSearchVideos) {
            formationVideoRepository.deleteByFormation_IdAndUrlYoutubeContainingIgnoreCase(
                    formationId,
                    "youtube.com/results"
            );
        }

        List<FormationVideo> currentVideos =
                formationVideoRepository.findByFormation_IdOrderByOrdreAsc(formationId);

        boolean hasGoodVideos = currentVideos.stream()
                .anyMatch(video -> isEmbeddableYoutubeUrl(video.getUrlYoutube()));

        if (hasGoodVideos) {
            return;
        }

        List<RecommendedVideoDto> videos = parseVideos(recommendation);

        if (videos.isEmpty()) {
            videos = defaultVideosFromRecommendation(recommendation);
        }

        int ordre = 1;

        for (RecommendedVideoDto videoDto : videos) {
            String url = cleanValue(videoDto.getUrlYoutube(), "");

            if (!isEmbeddableYoutubeUrl(url)) {
                url = defaultYoutubeUrl(recommendation);
            }

            FormationVideo video = new FormationVideo();
            video.setFormation(formation);
            video.setTitre(cleanValue(videoDto.getTitre(), "Vidéo " + ordre + " - " + formation.getTitre()));
            video.setUrlYoutube(url);
            video.setOrdre(videoDto.getOrdre() == null ? ordre : videoDto.getOrdre());

            formationVideoRepository.save(video);
            ordre++;
        }
    }

    private List<RecommendedVideoDto> parseVideos(FormationRecommendation recommendation) {
        if (recommendation.getVideosJson() == null || recommendation.getVideosJson().isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<RecommendedVideoDto> videos = objectMapper.readValue(
                    recommendation.getVideosJson(),
                    new TypeReference<List<RecommendedVideoDto>>() {}
            );

            if (videos == null) {
                return new ArrayList<>();
            }

            return videos;
        } catch (Exception e) {
            System.out.println("Impossible de parser videosJson recommandation id="
                    + recommendation.getId()
                    + " : "
                    + e.getMessage());

            return new ArrayList<>();
        }
    }

    private List<RecommendedVideoDto> defaultVideosFromRecommendation(FormationRecommendation recommendation) {
        String text = (
                safe(recommendation.getFormationTitle()) + " " +
                safe(recommendation.getDescription()) + " " +
                safe(recommendation.getMatchedSkills()) + " " +
                safe(recommendation.getPoste())
        ).toLowerCase();

        if (text.contains("recrutement") || text.contains("talent")) {
            return List.of(
                    new RecommendedVideoDto("Recrutement RH", "https://www.youtube.com/watch?v=HG68Ymazo18", 1),
                    new RecommendedVideoDto("Entretien de recrutement", "https://www.youtube.com/watch?v=6G8_qA8M8pQ", 2),
                    new RecommendedVideoDto("Sourcing candidats", "https://www.youtube.com/watch?v=4FQY3u4UxS0", 3)
            );
        }

        if (text.contains("paie") || text.contains("salaire")) {
            return List.of(
                    new RecommendedVideoDto("Gestion de la paie", "https://www.youtube.com/watch?v=b7OXULhF1pc", 1),
                    new RecommendedVideoDto("Bulletin de paie expliqué", "https://www.youtube.com/watch?v=zE51pYOTp2s", 2),
                    new RecommendedVideoDto("Charges sociales et salaire net", "https://www.youtube.com/watch?v=5cI-AkKy66I", 3)
            );
        }

        if (text.contains("droit") || text.contains("contrat")) {
            return List.of(
                    new RecommendedVideoDto("Droit du travail", "https://www.youtube.com/watch?v=4Ko4b38N7gE", 1),
                    new RecommendedVideoDto("Contrat de travail", "https://www.youtube.com/watch?v=O_4LwZ2pJzQ", 2),
                    new RecommendedVideoDto("Droit social RH", "https://www.youtube.com/watch?v=R6NoL7cnkQY", 3)
            );
        }

        if (text.contains("leadership") || text.contains("management") || text.contains("équipe") || text.contains("equipe")) {
            return List.of(
                    new RecommendedVideoDto("Leadership", "https://www.youtube.com/watch?v=ktlTxC4QG8g", 1),
                    new RecommendedVideoDto("Gestion équipe", "https://www.youtube.com/watch?v=4a0FbQdH3dY", 2),
                    new RecommendedVideoDto("Management et leadership", "https://www.youtube.com/watch?v=Q2vQkHjS4xQ", 3)
            );
        }

        return List.of(
                new RecommendedVideoDto("Communication professionnelle", "https://www.youtube.com/watch?v=HAnw168huqA", 1),
                new RecommendedVideoDto("Communication efficace au travail", "https://www.youtube.com/watch?v=8sjA90hvnQ0", 2),
                new RecommendedVideoDto("Développement des compétences", "https://www.youtube.com/watch?v=Q2vQkHjS4xQ", 3)
        );
    }

    private String defaultYoutubeUrl(FormationRecommendation recommendation) {
        String text = (
                safe(recommendation.getFormationTitle()) + " " +
                safe(recommendation.getDescription()) + " " +
                safe(recommendation.getMatchedSkills())
        ).toLowerCase();

        if (text.contains("recrutement") || text.contains("talent")) {
            return "https://www.youtube.com/watch?v=HG68Ymazo18";
        }

        if (text.contains("paie") || text.contains("salaire")) {
            return "https://www.youtube.com/watch?v=b7OXULhF1pc";
        }

        if (text.contains("droit") || text.contains("contrat")) {
            return "https://www.youtube.com/watch?v=4Ko4b38N7gE";
        }

        if (text.contains("leadership") || text.contains("management")) {
            return "https://www.youtube.com/watch?v=ktlTxC4QG8g";
        }

        return "https://www.youtube.com/watch?v=HAnw168huqA";
    }

    private boolean isEmbeddableYoutubeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String lower = url.toLowerCase();

        return lower.contains("youtube.com/watch?v=")
                || lower.contains("youtu.be/")
                || lower.contains("youtube.com/embed/")
                || lower.contains("youtube.com/shorts/");
    }

    private String cleanValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // =========================================================
    // AUTH
    // =========================================================

    private Employe getCurrentEmploye(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        Jwt jwt = (Jwt) auth.getPrincipal();

        String tokenEmail = jwt.getClaimAsString("email");

        if (tokenEmail == null || tokenEmail.isBlank()) {
            tokenEmail = jwt.getClaimAsString("preferred_username");
        }

        if (tokenEmail == null || tokenEmail.isBlank()) {
            throw new RuntimeException("Email utilisateur introuvable dans le token");
        }

        final String finalEmail = tokenEmail;

        return employeRepository.findByEmail(finalEmail)
                .orElseThrow(() -> new RuntimeException("Employé introuvable pour email : " + finalEmail));
    }
}