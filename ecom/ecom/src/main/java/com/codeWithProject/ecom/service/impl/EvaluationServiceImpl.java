package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Evaluation;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.EvaluationRepository;
import com.codeWithProject.ecom.service.EvaluationService;
import com.codeWithProject.ecom.service.dto.EvaluationDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.EvaluationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EmployeRepository employeRepository;
    private final EvaluationMapper mapper;

    // ==========================================================
    // LEGACY / ADMIN
    // ==========================================================

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findAll() {
        return evaluationRepository.findAll(Sort.by(Sort.Direction.DESC, "dateEvaluation"))
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EvaluationDTO> findAll(Pageable pageable) {
        Page<Evaluation> page = evaluationRepository.findAll(pageable);

        List<EvaluationDTO> dtos = page.getContent()
                .stream()
                .map(mapper::toDto)
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvaluationDTO> findById(Long id) {
        return evaluationRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findByEmployeId(Long employeId) {
        if (employeId == null) {
            return List.of();
        }

        return evaluationRepository.findByEmploye_IdOrderByDateEvaluationDesc(employeId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationDTO create(EvaluationDTO dto) {
        validateEvaluationDto(dto);

        Evaluation evaluation = mapper.toEntity(dto);

        if (evaluation.getDateEvaluation() == null) {
            evaluation.setDateEvaluation(LocalDate.now());
        }

        Evaluation saved = evaluationRepository.save(evaluation);

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public EvaluationDTO update(Long id, EvaluationDTO dto) {
        Evaluation existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));

        applyBasicUpdates(existing, dto);

        if (dto.getEmployeId() != null) {
            Employe employe = getEmploye(dto.getEmployeId());
            existing.setEmploye(employe);
        }

        if (dto.getEvaluateurId() != null) {
            Employe evaluateur = getEmploye(dto.getEvaluateurId());
            existing.setEvaluateur(evaluateur);
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
        if (employeId == null) {
            return 0.0;
        }

        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);

        Double moyenne = evaluationRepository.moyenneEvaluationSurPeriode(employeId, debut, fin);

        return moyenne != null ? moyenne : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getMoyenneDernieresEvaluations(Long employeId, int nombre) {
        if (employeId == null || nombre <= 0) {
            return 0.0;
        }

        List<Evaluation> dernieres = evaluationRepository.findByEmploye_IdOrderByDateEvaluationDesc(employeId)
                .stream()
                .limit(nombre)
                .toList();

        if (dernieres.isEmpty()) {
            return 0.0;
        }

        return dernieres.stream()
                .map(Evaluation::getNote)
                .filter(note -> note != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    // ==========================================================
    // ADMIN RH
    // ==========================================================

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findAllForAdminRh() {
        return findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatsGlobales() {
        List<Evaluation> evaluations = evaluationRepository.findAll();

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalEvaluations", evaluations.size());
        stats.put("moyenneNote", safeDouble(evaluationRepository.moyenneGlobale()));
        stats.put("moyenneObjectifs", safeDouble(evaluationRepository.moyenneObjectifsGlobale()));
        stats.put("excellentesEvaluations", safeLong(evaluationRepository.countByNoteGreaterThanOrEqual(8.5)));
        stats.put("evaluationsFaibles", safeLong(evaluationRepository.countByNoteLessThan(5.0)));

        double meilleureNote = evaluations.stream()
                .map(Evaluation::getNote)
                .filter(note -> note != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        double plusFaibleNote = evaluations.stream()
                .map(Evaluation::getNote)
                .filter(note -> note != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        stats.put("meilleureNote", meilleureNote);
        stats.put("plusFaibleNote", plusFaibleNote);

        return stats;
    }



    @Override
@Transactional
public EvaluationDTO createByAdminRhForManager(EvaluationDTO dto, Long adminRhId) {
    validateEvaluationDto(dto);

    Employe adminRh = getEmploye(adminRhId);

    if (!isAdminRh(adminRh)) {
        throw new BusinessException("Seul un administrateur RH peut évaluer un manager");
    }

    Employe manager = getEmploye(dto.getEmployeId());

    if (!isManager(manager)) {
        throw new BusinessException("L'employé sélectionné n'est pas un manager");
    }

    if (adminRh.getId().equals(manager.getId())) {
        throw new BusinessException("Un administrateur RH ne peut pas s'évaluer lui-même");
    }

    Evaluation evaluation = Evaluation.builder()
            .employe(manager)
            .evaluateur(adminRh)
            .dateEvaluation(dto.getDateEvaluation() != null ? dto.getDateEvaluation() : LocalDate.now())
            .periode(dto.getPeriode())

            .note(dto.getNote())
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
            .statut(normalizeStatut(dto.getStatut()))
            .build();

    Evaluation saved = evaluationRepository.save(evaluation);

    log.info("Évaluation manager créée par adminRhId={} pour managerId={}", adminRhId, manager.getId());

    return mapper.toDto(saved);
}

    // ==========================================================
    // MANAGER
    // ==========================================================

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findByManagerId(Long managerId) {
        if (managerId == null) {
            return List.of();
        }

        return evaluationRepository.findByEmploye_Manager_IdOrderByDateEvaluationDesc(managerId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationDTO createByManager(EvaluationDTO dto, Long managerId) {
        validateEvaluationDto(dto);

        Employe manager = getManager(managerId);
        Employe employe = getEmploye(dto.getEmployeId());

        assertEmployeBelongsToManager(employe, manager);

        if (manager.getId().equals(employe.getId())) {
            throw new BusinessException("Un manager ne peut pas s'évaluer lui-même");
        }

        Evaluation evaluation = Evaluation.builder()
        .employe(employe)
        .evaluateur(manager)
        .dateEvaluation(dto.getDateEvaluation() != null ? dto.getDateEvaluation() : LocalDate.now())
        .periode(dto.getPeriode())

        .note(dto.getNote())
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
        .statut(normalizeStatut(dto.getStatut()))
        .build();
        Evaluation saved = evaluationRepository.save(evaluation);

        log.info("Évaluation créée par managerId={} pour employeId={}", managerId, employe.getId());

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public EvaluationDTO updateByManager(Long id, EvaluationDTO dto, Long managerId) {
        Evaluation existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));

        Employe manager = getManager(managerId);

        assertEvaluationEditableByManager(existing, manager);

        if (dto.getEmployeId() != null && !dto.getEmployeId().equals(existing.getEmploye().getId())) {
            Employe newEmploye = getEmploye(dto.getEmployeId());

            assertEmployeBelongsToManager(newEmploye, manager);

            if (manager.getId().equals(newEmploye.getId())) {
                throw new BusinessException("Un manager ne peut pas s'évaluer lui-même");
            }

            existing.setEmploye(newEmploye);
        }

        applyBasicUpdates(existing, dto);
        existing.setEvaluateur(manager);

        Evaluation updated = evaluationRepository.save(existing);

        log.info("Évaluation modifiée par managerId={}, evaluationId={}", managerId, id);

        return mapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteByManager(Long id, Long managerId) {
        Evaluation existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));

        Employe manager = getManager(managerId);

        assertEvaluationEditableByManager(existing, manager);

        evaluationRepository.delete(existing);

        log.info("Évaluation supprimée par managerId={}, evaluationId={}", managerId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatsManager(Long managerId) {
        if (managerId == null) {
            return Map.of();
        }

        List<Evaluation> evaluations = evaluationRepository.findByEmploye_Manager_IdOrderByDateEvaluationDesc(managerId);

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalEvaluations", evaluations.size());
        stats.put("moyenneNote", safeDouble(evaluationRepository.moyenneEquipeManager(managerId)));
        stats.put("moyenneObjectifs", safeDouble(evaluationRepository.moyenneObjectifsManager(managerId)));
        stats.put("excellentesEvaluations", safeLong(evaluationRepository.countManagerByNoteGreaterThanOrEqual(managerId, 8.5)));
        stats.put("evaluationsFaibles", safeLong(evaluationRepository.countManagerByNoteLessThan(managerId, 5.0)));

        double meilleureNote = evaluations.stream()
                .map(Evaluation::getNote)
                .filter(note -> note != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        double plusFaibleNote = evaluations.stream()
                .map(Evaluation::getNote)
                .filter(note -> note != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        stats.put("meilleureNote", meilleureNote);
        stats.put("plusFaibleNote", plusFaibleNote);

        return stats;
    }

    // ==========================================================
    // EMPLOYÉ
    // ==========================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatsEmploye(Long employeId) {
        if (employeId == null) {
            return Map.of();
        }

        List<Evaluation> evaluations = evaluationRepository.findByEmploye_IdOrderByDateEvaluationDesc(employeId);

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalEvaluations", evaluations.size());
        stats.put("moyenneNote", safeDouble(evaluationRepository.moyenneGlobaleEmploye(employeId)));
        stats.put("moyenneObjectifs", safeDouble(evaluationRepository.moyenneObjectifsEmploye(employeId)));

        Evaluation derniere = evaluations.isEmpty() ? null : evaluations.get(0);

        stats.put("derniereNote", derniere != null && derniere.getNote() != null ? derniere.getNote() : 0.0);
        stats.put("derniereEvaluation", derniere != null ? mapper.toDto(derniere) : null);

        return stats;
    }

    // ==========================================================
    // HELPERS VALIDATION
    // ==========================================================

    private void validateEvaluationDto(EvaluationDTO dto) {
        if (dto == null) {
            throw new BusinessException("Données d'évaluation invalides");
        }

        if (dto.getEmployeId() == null) {
            throw new BusinessException("L'employé évalué est obligatoire");
        }

        if (dto.getNote() == null) {
            throw new BusinessException("La note est obligatoire");
        }

        validateNote(dto.getNote());

        if (dto.getObjectifsAtteints() != null) {
            validateObjectifs(dto.getObjectifsAtteints());
        }
    }

    private void validateNote(Double note) {
        if (note == null || note < 0 || note > 10) {
            throw new BusinessException("La note doit être comprise entre 0 et 10");
        }
    }

    private void validateObjectifs(Integer objectifsAtteints) {
        if (objectifsAtteints == null) {
            return;
        }

        if (objectifsAtteints < 0 || objectifsAtteints > 100) {
            throw new BusinessException("Les objectifs atteints doivent être compris entre 0 et 100");
        }
    }

    private void applyBasicUpdates(Evaluation evaluation, EvaluationDTO dto) {
        if (evaluation == null) {
            throw new BusinessException("Évaluation invalide");
        }

        if (dto == null) {
            throw new BusinessException("Données d'évaluation invalides");
        }

        if (dto.getDateEvaluation() != null) {
            evaluation.setDateEvaluation(dto.getDateEvaluation());
        }

        if (dto.getNote() != null) {
            validateNote(dto.getNote());
            evaluation.setNote(dto.getNote());
        }

        if (dto.getObjectifsAtteints() != null) {
            validateObjectifs(dto.getObjectifsAtteints());
            evaluation.setObjectifsAtteints(dto.getObjectifsAtteints());
        }

        if (dto.getCommentaire() != null) {
            evaluation.setCommentaire(dto.getCommentaire());
        }

        if (dto.getPointsForts() != null) {
    evaluation.setPointsForts(dto.getPointsForts());
}

if (dto.getAxesAmelioration() != null) {
    evaluation.setAxesAmelioration(dto.getAxesAmelioration());
}

if (dto.getCommentaireManager() != null) {
    evaluation.setCommentaireManager(dto.getCommentaireManager());
}

if (dto.getObjectifs() != null) {
    evaluation.setObjectifs(dto.getObjectifs());
}
if (dto.getPeriode() != null) {
    evaluation.setPeriode(dto.getPeriode());
}


if (dto.getNoteTechnique() != null) {
    evaluation.setNoteTechnique(dto.getNoteTechnique());
}

if (dto.getNoteCommunication() != null) {
    evaluation.setNoteCommunication(dto.getNoteCommunication());
}

if (dto.getNoteLeadership() != null) {
    evaluation.setNoteLeadership(dto.getNoteLeadership());
}

if (dto.getNotePonctualite() != null) {
    evaluation.setNotePonctualite(dto.getNotePonctualite());
}

if (dto.getNoteProductivite() != null) {
    evaluation.setNoteProductivite(dto.getNoteProductivite());
}

 if (dto.getStatut() != null) {
        evaluation.setStatut(normalizeStatut(dto.getStatut()));
    }
    }

    // ==========================================================
    // HELPERS ACCÈS MÉTIER
    // ==========================================================

    private Employe getEmploye(Long employeId) {
        if (employeId == null) {
            throw new BusinessException("Employé obligatoire");
        }

        return employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));
    }

    private Employe getManager(Long managerId) {
        Employe manager = getEmploye(managerId);

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new BusinessException("L'utilisateur connecté n'est pas un manager");
        }

        return manager;
    }

    private void assertEmployeBelongsToManager(Employe employe, Employe manager) {
        if (employe == null || manager == null) {
            throw new BusinessException("Employé ou manager invalide");
        }

        if (employe.getManager() == null || employe.getManager().getId() == null) {
            throw new BusinessException("Cet employé n'est assigné à aucun manager");
        }

        if (!employe.getManager().getId().equals(manager.getId())) {
            throw new BusinessException("Cet employé n'appartient pas à votre équipe");
        }
    }

    private void assertEvaluationEditableByManager(Evaluation evaluation, Employe manager) {
        if (evaluation == null || manager == null) {
            throw new BusinessException("Évaluation ou manager invalide");
        }

        if (evaluation.getEvaluateur() == null || evaluation.getEvaluateur().getId() == null) {
            throw new BusinessException("Cette évaluation n'a pas d'évaluateur");
        }

        if (!evaluation.getEvaluateur().getId().equals(manager.getId())) {
            throw new BusinessException("Vous ne pouvez modifier que les évaluations que vous avez créées");
        }

        assertEmployeBelongsToManager(evaluation.getEmploye(), manager);
    }

    // ==========================================================
    // HELPERS STATS
    // ==========================================================

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }




private boolean isAdminRh(Employe employe) {
    if (employe == null) {
        return false;
    }

    String role = employe.getRole() != null ? employe.getRole() : "";

    return "ADMIN_RH".equalsIgnoreCase(role)
            || "ADMIN".equalsIgnoreCase(role);
}

private boolean isManager(Employe employe) {
    if (employe == null) {
        return false;
    }

    String role = employe.getRole() != null ? employe.getRole() : "";

    return "MANAGER".equalsIgnoreCase(role);
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