package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Candidature;
import com.codeWithProject.ecom.repository.CandidatureRepository;
import com.codeWithProject.ecom.service.CandidatureService;
import com.codeWithProject.ecom.service.CvStorageService;
import com.codeWithProject.ecom.service.dto.ApiMessageResponseDTO;
import com.codeWithProject.ecom.service.dto.CandidatureResponseDTO;
import com.codeWithProject.ecom.service.dto.DecisionCandidatureRequestDTO;
import com.codeWithProject.ecom.service.dto.TopCandidatureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidatureController {

    private final CandidatureService candidatureService;
    private final CandidatureRepository candidatureRepository;
    private final CvStorageService cvStorageService;

    /**
     * L'employé postule à une offre avec :
     * - offreId
     * - employeId
     * - motivation
     * - fichier CV
     */
    @PostMapping(
            value = "/postuler",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CandidatureResponseDTO> postuler(
            @RequestParam Long offreId,
            @RequestParam Long employeId,
            @RequestParam(required = false) String motivation,
            @RequestParam("cv") MultipartFile cv
    ) {
        CandidatureResponseDTO response = candidatureService.postuler(
                offreId,
                employeId,
                motivation,
                cv
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les candidatures d'un employé.
     */
    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<CandidatureResponseDTO>> getMesCandidatures(
            @PathVariable Long employeId
    ) {
        return ResponseEntity.ok(candidatureService.getMesCandidatures(employeId));
    }

    /**
     * Vérifier si un employé a déjà postulé à une offre.
     */
    @GetMapping("/exists")
    public ResponseEntity<ApiMessageResponseDTO> hasAlreadyApplied(
            @RequestParam Long offreId,
            @RequestParam Long employeId
    ) {
        boolean exists = candidatureService.hasAlreadyApplied(offreId, employeId);

        return ResponseEntity.ok(
                ApiMessageResponseDTO.builder()
                        .success(exists)
                        .message(
                                exists
                                        ? "L'employé a déjà postulé à cette offre."
                                        : "L'employé n'a pas encore postulé à cette offre."
                        )
                        .build()
        );
    }

    /**
     * Récupérer toutes les candidatures liées à une offre.
     */
    @GetMapping("/offre/{offreId}")
    public ResponseEntity<List<CandidatureResponseDTO>> getCandidaturesByOffre(
            @PathVariable Long offreId
    ) {
        return ResponseEntity.ok(candidatureService.getCandidaturesByOffre(offreId));
    }

    /**
     * Récupérer le top des candidatures selon le score IA.
     * Par défaut : top 5.
     */
    @GetMapping("/offre/{offreId}/top")
    public ResponseEntity<List<TopCandidatureResponse>> getTopCandidatures(
            @PathVariable Long offreId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(candidatureService.getTopCandidatures(offreId, limit));
    }

    /**
     * Accepter une candidature.
     * Le service change le statut en ACCEPTEE
     * et met à jour le poste de l'employé selon le titre de l'offre.
     */
    @PostMapping("/{candidatureId}/accepter")
    public ResponseEntity<CandidatureResponseDTO> accepterCandidature(
            @PathVariable Long candidatureId,
            @RequestBody(required = false) DecisionCandidatureRequestDTO request
    ) {
        return ResponseEntity.ok(
                candidatureService.accepterCandidature(candidatureId, request)
        );
    }

    /**
     * Refuser une candidature.
     */
    @PostMapping("/{candidatureId}/refuser")
    public ResponseEntity<CandidatureResponseDTO> refuserCandidature(
            @PathVariable Long candidatureId,
            @RequestBody(required = false) DecisionCandidatureRequestDTO request
    ) {
        return ResponseEntity.ok(
                candidatureService.refuserCandidature(candidatureId, request)
        );
    }

    /**
     * Relancer l'analyse IA pour une candidature.
     */
    @PostMapping("/{candidatureId}/relancer-analyse")
    public ResponseEntity<CandidatureResponseDTO> relancerAnalyseIa(
            @PathVariable Long candidatureId
    ) {
        return ResponseEntity.ok(
                candidatureService.relancerAnalyseIa(candidatureId)
        );
    }

    /**
     * Télécharger / afficher le CV d'une candidature.
     * Cette URL correspond au champ cvUrl généré dans CandidatureResponseDTO :
     * /api/candidatures/{id}/cv
     */
    @GetMapping("/{candidatureId}/cv")
    public ResponseEntity<Resource> getCv(
            @PathVariable Long candidatureId
    ) {
        try {
            Candidature candidature = candidatureRepository.findById(candidatureId)
                    .orElseThrow(() -> new RuntimeException(
                            "Candidature introuvable avec id : " + candidatureId
                    ));

            if (candidature.getCvFileName() == null) {
                throw new RuntimeException("Aucun CV associé à cette candidature.");
            }

            Path cvPath = cvStorageService.getCvPath(candidature.getCvFileName());
            Resource resource = new UrlResource(cvPath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Le fichier CV est introuvable ou illisible.");
            }

            String contentType = candidature.getCvContentType() != null
                    ? candidature.getCvContentType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            String originalFileName = candidature.getCvOriginalName() != null
                    ? candidature.getCvOriginalName()
                    : candidature.getCvFileName();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + originalFileName + "\""
                    )
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement du CV : " + e.getMessage(), e);
        }
    }
}