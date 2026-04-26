package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Evaluation;
import com.codeWithProject.ecom.repository.EvaluationRepository;
import com.codeWithProject.ecom.service.EvaluationService;
import com.codeWithProject.ecom.service.dto.EvaluationDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.EvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findAll() {
        return evaluationRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EvaluationDTO> findAll(Pageable pageable) {
        Page<Evaluation> page = evaluationRepository.findAll(pageable);
        List<EvaluationDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvaluationDTO> findById(Long id) {
        return evaluationRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findByEmployeId(Long employeId) {
        return evaluationRepository.findByEmployeIdOrderByDateEvaluationDesc(employeId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EvaluationDTO create(EvaluationDTO dto) {
        Evaluation evaluation = mapper.toEntity(dto);
        Evaluation saved = evaluationRepository.save(evaluation);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public EvaluationDTO update(Long id, EvaluationDTO dto) {
        Evaluation existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));
        existing.setDateEvaluation(dto.getDateEvaluation());
        existing.setNote(dto.getNote());
        existing.setObjectifsAtteints(dto.getObjectifsAtteints());
        existing.setCommentaire(dto.getCommentaire());
        // Mise à jour des relations (attention : simplification)
        if (dto.getEmployeId() != null) {
            existing.setEmploye(mapper.toEntity(dto).getEmploye());
        }
        if (dto.getEvaluateurId() != null) {
            existing.setEvaluateur(mapper.toEntity(dto).getEvaluateur());
        }
        Evaluation updated = evaluationRepository.save(existing);
        return mapper.toDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!evaluationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Evaluation", id);
        }
        evaluationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getMoyenneNotes(Long employeId, int annee) {
        return evaluationRepository.moyenneEvaluationAnnuelle(employeId, annee);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getMoyenneDernieresEvaluations(Long employeId, int nombre) {
        // Récupère les 'nombre' dernières évaluations et calcule la moyenne des notes
        List<Evaluation> dernieres = evaluationRepository.findByEmployeIdOrderByDateEvaluationDesc(employeId)
                .stream()
                .limit(nombre)
                .collect(Collectors.toList());
        if (dernieres.isEmpty()) {
            return null;
        }
        return dernieres.stream()
                .mapToDouble(Evaluation::getNote)
                .average()
                .orElse(0.0);
    }
}