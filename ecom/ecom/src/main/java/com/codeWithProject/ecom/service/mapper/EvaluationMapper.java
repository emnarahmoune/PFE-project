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
        if (entity == null) return null;
        EvaluationDTO dto = EvaluationDTO.builder()
                .id(entity.getId())
                .dateEvaluation(entity.getDateEvaluation())
                .note(entity.getNote())
                .objectifsAtteints(entity.getObjectifsAtteints())
                .commentaire(entity.getCommentaire())
                .build();
        if (entity.getEmploye() != null) {
            dto.setEmployeId(entity.getEmploye().getId());
            dto.setEmployeNom(entity.getEmploye().getNom());
            dto.setEmployePrenom(entity.getEmploye().getPrenom());
        }
        if (entity.getEvaluateur() != null) {
            dto.setEvaluateurId(entity.getEvaluateur().getId());
            dto.setEvaluateurNom(entity.getEvaluateur().getNom() + " " + entity.getEvaluateur().getPrenom());
        }
        return dto;
    }

    public Evaluation toEntity(EvaluationDTO dto) {
        if (dto == null) return null;
        Evaluation.EvaluationBuilder builder = Evaluation.builder()
                .id(dto.getId())
                .dateEvaluation(dto.getDateEvaluation())
                .note(dto.getNote())
                .objectifsAtteints(dto.getObjectifsAtteints())
                .commentaire(dto.getCommentaire());
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
}