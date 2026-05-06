package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.EvaluationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EvaluationService {

    // =========================
    // LEGACY / ADMIN
    // =========================

    List<EvaluationDTO> findAll();

    Page<EvaluationDTO> findAll(Pageable pageable);

    Optional<EvaluationDTO> findById(Long id);

    List<EvaluationDTO> findByEmployeId(Long employeId);

    EvaluationDTO create(EvaluationDTO dto);

    EvaluationDTO update(Long id, EvaluationDTO dto);

    void delete(Long id);

    Double getMoyenneNotes(Long employeId, int annee);

    Double getMoyenneDernieresEvaluations(Long employeId, int nombre);

    // =========================
    // ADMIN RH
    // =========================

    List<EvaluationDTO> findAllForAdminRh();

    Map<String, Object> getStatsGlobales();

    // =========================
    // MANAGER
    // =========================

    List<EvaluationDTO> findByManagerId(Long managerId);

    EvaluationDTO createByManager(EvaluationDTO dto, Long managerId);

    EvaluationDTO updateByManager(Long id, EvaluationDTO dto, Long managerId);

    void deleteByManager(Long id, Long managerId);

    Map<String, Object> getStatsManager(Long managerId);

    // =========================
    // EMPLOYÉ
    // =========================

    Map<String, Object> getStatsEmploye(Long employeId);

EvaluationDTO createByAdminRhForManager(EvaluationDTO dto, Long adminRhId);


}