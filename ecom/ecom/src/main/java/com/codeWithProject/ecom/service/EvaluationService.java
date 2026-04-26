package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.EvaluationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface EvaluationService {
    List<EvaluationDTO> findAll();
    Page<EvaluationDTO> findAll(Pageable pageable);
    Optional<EvaluationDTO> findById(Long id);
    List<EvaluationDTO> findByEmployeId(Long employeId);
    EvaluationDTO create(EvaluationDTO dto);
    EvaluationDTO update(Long id, EvaluationDTO dto);
    void delete(Long id);
    Double getMoyenneNotes(Long employeId, int annee);
    Double getMoyenneDernieresEvaluations(Long employeId, int nombre);
}