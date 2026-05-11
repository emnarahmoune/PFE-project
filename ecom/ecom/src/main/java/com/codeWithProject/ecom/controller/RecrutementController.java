package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.service.RecrutementService;
import com.codeWithProject.ecom.service.dto.ApiMessageResponseDTO;
import com.codeWithProject.ecom.service.dto.ChangementStatutOffreRequestDTO;
import com.codeWithProject.ecom.service.dto.OffreRecrutementRequest;
import com.codeWithProject.ecom.service.dto.OffreRecrutementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecrutementController {

    private final RecrutementService recrutementService;

    /**
     * Récupérer toutes les offres de recrutement interne.
     */
    @GetMapping("/offres")
    public ResponseEntity<List<OffreRecrutementResponse>> getAllOffres() {
        return ResponseEntity.ok(recrutementService.getAllOffres());
    }

    /**
     * Récupérer uniquement les offres ouvertes.
     * Ces offres sont visibles par les employés.
     */
    @GetMapping("/offres/ouvertes")
    public ResponseEntity<List<OffreRecrutementResponse>> getOffresOuvertes() {
        return ResponseEntity.ok(recrutementService.getOffresOuvertes());
    }

    /**
     * Récupérer une offre par ID.
     */
    @GetMapping("/offres/{id}")
    public ResponseEntity<OffreRecrutementResponse> getOffreById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(recrutementService.getOffreById(id));
    }

    /**
     * Créer une nouvelle offre de recrutement interne.
     */
    @PostMapping("/offres")
    public ResponseEntity<OffreRecrutementResponse> createOffre(
            @RequestBody OffreRecrutementRequest request
    ) {
        return ResponseEntity.ok(recrutementService.createOffre(request));
    }

    /**
     * Modifier une offre existante.
     */
    @PutMapping("/offres/{id}")
    public ResponseEntity<OffreRecrutementResponse> updateOffre(
            @PathVariable Long id,
            @RequestBody OffreRecrutementRequest request
    ) {
        return ResponseEntity.ok(recrutementService.updateOffre(id, request));
    }

    /**
     * Changer le statut d'une offre.
     * Exemple : BROUILLON -> OUVERTE -> FERMEE.
     */
@PutMapping("/offres/{id}/statut")
public ResponseEntity<OffreRecrutementResponse> changerStatutOffre(
        @PathVariable Long id,
        @RequestBody ChangementStatutOffreRequestDTO request
) {
    return ResponseEntity.ok(recrutementService.changerStatutOffre(id, request));
}

    /**
     * Supprimer une offre.
     */
    @DeleteMapping("/offres/{id}")
    public ResponseEntity<ApiMessageResponseDTO> deleteOffre(
            @PathVariable Long id
    ) {
        recrutementService.deleteOffre(id);

        return ResponseEntity.ok(
                ApiMessageResponseDTO.builder()
                        .success(true)
                        .message("Offre supprimée avec succès.")
                        .build()
        );
    }
}