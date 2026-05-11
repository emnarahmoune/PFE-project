package com.codeWithProject.ecom.service.impl;




import java.util.stream.Collectors;
import com.codeWithProject.ecom.service.dto.ChangementStatutOffreRequestDTO;
import com.codeWithProject.ecom.service.dto.OffreRecrutementRequest;
import com.codeWithProject.ecom.service.dto.OffreRecrutementResponse;
import com.codeWithProject.ecom.entity.OffreRecrutement;
import com.codeWithProject.ecom.entity.RecrutementScore;
import com.codeWithProject.ecom.entity.enums.StatutOffreRecrutement;
import com.codeWithProject.ecom.repository.OffreRecrutementRepository;
import com.codeWithProject.ecom.repository.RecrutementScoreRepository;
import com.codeWithProject.ecom.service.FormationRecommendationAutoService;
import com.codeWithProject.ecom.service.RecrutementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecrutementServiceImpl implements RecrutementService {

    private final OffreRecrutementRepository offreRecrutementRepository;
    private final RecrutementScoreRepository recrutementScoreRepository;

    private final FormationRecommendationAutoService formationRecommendationAutoService;
    @Override
    @Transactional(readOnly = true)
    public List<OffreRecrutementResponse> getAllOffres() {
        return offreRecrutementRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OffreRecrutementResponse> getOffresOuvertes() {
        return offreRecrutementRepository
                .findByStatutOrderByDatePublicationDesc(StatutOffreRecrutement.OUVERTE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OffreRecrutementResponse getOffreById(Long id) {
        OffreRecrutement offre = offreRecrutementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre de recrutement introuvable avec id : " + id));

        return toResponse(offre);
    }

    @Override
    public OffreRecrutementResponse createOffre(OffreRecrutementRequest request) {
        validateRequest(request);

        OffreRecrutement offre = OffreRecrutement.builder()
                .titrePoste(request.getTitrePoste())
                .description(request.getDescription())
                .departement(request.getDepartement())
                .typeContrat(request.getTypeContrat())
                .localisation(request.getLocalisation())
                .competencesRequises(safeList(request.getCompetencesRequises()))
                .technologiesRequises(safeList(request.getTechnologiesRequises()))
                .experienceMin(request.getExperienceMin())
                .niveauEtude(request.getNiveauEtude())
                .dateExpiration(request.getDateExpiration())
                .statut(StatutOffreRecrutement.BROUILLON)
                .salairePropose(request.getSalairePropose())
                .build();

        OffreRecrutement saved = offreRecrutementRepository.save(offre);


        return toResponse(saved);
    }

   @Override
public OffreRecrutementResponse updateOffre(Long id, OffreRecrutementRequest request) {
    validateRequest(request);

    OffreRecrutement offre = offreRecrutementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Offre de recrutement introuvable avec id : " + id));

    offre.setTitrePoste(request.getTitrePoste());
    offre.setDescription(request.getDescription());
    offre.setDepartement(request.getDepartement());
    offre.setTypeContrat(request.getTypeContrat());
    offre.setLocalisation(request.getLocalisation());

    offre.getCompetencesRequises().clear();
    offre.getCompetencesRequises().addAll(safeList(request.getCompetencesRequises()));

    offre.getTechnologiesRequises().clear();
    offre.getTechnologiesRequises().addAll(safeList(request.getTechnologiesRequises()));

    offre.setExperienceMin(request.getExperienceMin());
    offre.setNiveauEtude(request.getNiveauEtude());
    offre.setDateExpiration(request.getDateExpiration());
    offre.setSalairePropose(request.getSalairePropose());

OffreRecrutement saved = offreRecrutementRepository.saveAndFlush(offre);


return toResponse(saved);
}
@Override
@Transactional
public OffreRecrutementResponse changerStatutOffre(
        Long id,
        ChangementStatutOffreRequestDTO request
) {
    if (request == null || request.getStatut() == null) {
        throw new RuntimeException("Le statut est obligatoire.");
    }

    OffreRecrutement offre = offreRecrutementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                    "Offre de recrutement introuvable avec id : " + id
            ));

    offre.setStatut(request.getStatut());

    if (
            request.getStatut() == StatutOffreRecrutement.OUVERTE
                    && offre.getDatePublication() == null
    ) {
        offre.setDatePublication(LocalDateTime.now());
    }

    return toResponse(offre);
}
    @Override
    public void deleteOffre(Long id) {
        if (!offreRecrutementRepository.existsById(id)) {
            throw new RuntimeException("Offre de recrutement introuvable avec id : " + id);
        }

        offreRecrutementRepository.deleteById(id);
    }

    private void validateRequest(OffreRecrutementRequest request) {
        if (request == null) {
            throw new RuntimeException("Les données de l'offre sont obligatoires.");
        }

        if (isBlank(request.getTitrePoste())) {
            throw new RuntimeException("Le titre du poste est obligatoire.");
        }

        if (isBlank(request.getDescription())) {
            throw new RuntimeException("La description est obligatoire.");
        }

        if (request.getCompetencesRequises() == null || request.getCompetencesRequises().isEmpty()) {
            throw new RuntimeException("Les compétences requises sont obligatoires.");
        }

        if (request.getTechnologiesRequises() == null || request.getTechnologiesRequises().isEmpty()) {
            throw new RuntimeException("Les technologies requises sont obligatoires.");
        }
    }

    private OffreRecrutementResponse toResponse(OffreRecrutement offre) {
        Integer nombreCandidatures = offre.getCandidatures() == null
                ? 0
                : offre.getCandidatures().size();

        Integer meilleurScore = recrutementScoreRepository.findByOffreIdOrderByScoreGlobalDesc(offre.getId())
                .stream()
                .max(Comparator.comparing(RecrutementScore::getScoreGlobal))
                .map(RecrutementScore::getScoreGlobal)
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
                .creeParId(offre.getCreeParId())
                .creeParNom(offre.getCreeParNom())
                .nombreCandidatures(nombreCandidatures)
                .meilleurScore(meilleurScore)
                .salairePropose(offre.getSalairePropose())
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
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}