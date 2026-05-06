package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Evaluation;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.dto.EvaluationDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationMapper {

    private final EmployeRepository employeRepository;

    public EvaluationDTO toDto(Evaluation entity) {
        if (entity == null) {
            return null;
        }

        EvaluationDTO dto = EvaluationDTO.builder()
                .id(entity.getId())
                .dateEvaluation(entity.getDateEvaluation())
                .periode(entity.getPeriode())

                .note(entity.getNote())
                .noteGlobale(entity.getNote())

                .noteTechnique(entity.getNoteTechnique())
                .noteCommunication(entity.getNoteCommunication())
                .noteLeadership(entity.getNoteLeadership())
                .notePonctualite(entity.getNotePonctualite())
                .noteProductivite(entity.getNoteProductivite())

                .objectifsAtteints(entity.getObjectifsAtteints())
                .commentaire(entity.getCommentaire())

                .pointsForts(entity.getPointsForts())
                .axesAmelioration(entity.getAxesAmelioration())
                .commentaireManager(entity.getCommentaireManager())
                .objectifs(entity.getObjectifs())
                .statut(entity.getStatut())

                .niveauPerformance(resolveNiveauPerformance(entity.getNote()))
                .build();

        Employe employe = entity.getEmploye();

        if (employe != null) {
            dto.setEmployeId(employe.getId());
            dto.setEmployeNom(employe.getNom());
            dto.setEmployePrenom(employe.getPrenom());
            dto.setEmployeEmail(employe.getEmail());
            dto.setEmployePoste(employe.getPoste());
            dto.setEmployeDepartement(employe.getDepartement());
        }

        Employe evaluateur = entity.getEvaluateur();

        if (evaluateur != null) {
            dto.setEvaluateurId(evaluateur.getId());
            dto.setEvaluateurNom(evaluateur.getNom());
            dto.setEvaluateurPrenom(evaluateur.getPrenom());
            dto.setEvaluateurEmail(evaluateur.getEmail());
            dto.setEvaluateurRole(evaluateur.getRole());

            // Aliases conservés pour l'ancien front qui affiche "manager"
            dto.setManagerId(evaluateur.getId());
            dto.setManagerNom(evaluateur.getNom());
            dto.setManagerPrenom(evaluateur.getPrenom());
            dto.setManagerEmail(evaluateur.getEmail());
        }

        return dto;
    }

    public Evaluation toEntity(EvaluationDTO dto) {
        if (dto == null) {
            return null;
        }

        Double note = dto.getNote() != null ? dto.getNote() : dto.getNoteGlobale();

        Evaluation.EvaluationBuilder builder = Evaluation.builder()
                .id(dto.getId())
                .dateEvaluation(dto.getDateEvaluation())
                .periode(dto.getPeriode())

                .note(note)
                .noteTechnique(dto.getNoteTechnique())
                .noteCommunication(dto.getNoteCommunication())
                .noteLeadership(dto.getNoteLeadership())
                .notePonctualite(dto.getNotePonctualite())
                .noteProductivite(dto.getNoteProductivite())

                .objectifsAtteints(dto.getObjectifsAtteints())
                .commentaire(dto.getCommentaire())

                .pointsForts(dto.getPointsForts())
                .axesAmelioration(dto.getAxesAmelioration())
                .commentaireManager(dto.getCommentaireManager())
                .objectifs(dto.getObjectifs())
                .statut(normalizeStatut(dto.getStatut()));

        if (dto.getEmployeId() != null) {
            Employe employe = employeRepository.findById(dto.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

            builder.employe(employe);
        }

        if (dto.getEvaluateurId() != null) {
            Employe evaluateur = employeRepository.findById(dto.getEvaluateurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Évaluateur", dto.getEvaluateurId()));

            builder.evaluateur(evaluateur);
        }

        return builder.build();
    }

    private String resolveNiveauPerformance(Double note) {
        if (note == null) {
            return "NON_EVALUE";
        }

        if (note >= 8.5) {
            return "EXCELLENT";
        }

        if (note >= 7.0) {
            return "BON";
        }

        if (note >= 5.0) {
            return "MOYEN";
        }

        return "FAIBLE";
    }


    private String normalizeStatut(String statut) {
    if (statut == null || statut.isBlank()) {
        return "PUBLIEE";
    }

    String value = statut.trim().toUpperCase();

    return switch (value) {
        case "BROUILLON", "PUBLIEE", "VALIDEE", "ARCHIVEE" -> value;
        default -> "PUBLIEE";
    };
}
}