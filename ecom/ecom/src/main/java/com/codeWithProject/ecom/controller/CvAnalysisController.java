package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Candidature;
import com.codeWithProject.ecom.repository.CandidatureRepository;
import com.codeWithProject.ecom.service.IaMatchingService;
import com.codeWithProject.ecom.service.dto.CvAnalyseResponseDTO;
import com.codeWithProject.ecom.service.dto.IaMatchingResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv-analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CvAnalysisController {

    private final IaMatchingService iaMatchingService;
    private final CandidatureRepository candidatureRepository;

    /**
     * Lancer l'analyse IA d'une candidature.
     * Le backend récupère la candidature,
     * lit le CV,
     * compare le CV avec l'offre,
     * calcule le score,
     * puis sauvegarde l'analyse CV et le score.
     */
    @PostMapping("/candidature/{candidatureId}/analyser")
    public ResponseEntity<IaMatchingResponseDTO> analyserCandidature(
            @PathVariable Long candidatureId
    ) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RuntimeException(
                        "Candidature introuvable avec id : " + candidatureId
                ));

        IaMatchingResponseDTO response = iaMatchingService.analyserCandidature(candidature);

        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer l'analyse CV sauvegardée d'une candidature.
     */
    @GetMapping("/candidature/{candidatureId}")
    public ResponseEntity<CvAnalyseResponseDTO> getAnalyseByCandidature(
            @PathVariable Long candidatureId
    ) {
        return ResponseEntity.ok(
                iaMatchingService.getAnalyseByCandidature(candidatureId)
        );
    }
}