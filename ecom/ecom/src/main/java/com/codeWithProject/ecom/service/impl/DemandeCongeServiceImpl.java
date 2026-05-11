package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.*;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.DemandeCongeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DemandeCongeServiceImpl implements DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final DemandeCongeMapper mapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final WorkflowService workflowService;

    private static final Set<DayOfWeek> WEEKEND = Set.of(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    private static final Set<LocalDate> JOURS_FERIES = new HashSet<>();

    static {
        JOURS_FERIES.add(LocalDate.of(2026, 1, 1));
        JOURS_FERIES.add(LocalDate.of(2026, 4, 6));
        JOURS_FERIES.add(LocalDate.of(2026, 5, 1));
        JOURS_FERIES.add(LocalDate.of(2026, 5, 8));
        JOURS_FERIES.add(LocalDate.of(2026, 5, 14));
        JOURS_FERIES.add(LocalDate.of(2026, 5, 25));
        JOURS_FERIES.add(LocalDate.of(2026, 7, 14));
        JOURS_FERIES.add(LocalDate.of(2026, 8, 15));
        JOURS_FERIES.add(LocalDate.of(2026, 11, 1));
        JOURS_FERIES.add(LocalDate.of(2026, 11, 11));
        JOURS_FERIES.add(LocalDate.of(2026, 12, 25));
    }

    private int calculerJoursOuvres(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            return 0;
        }

        int count = 0;
        LocalDate current = debut;

        while (!current.isAfter(fin)) {
            if (!WEEKEND.contains(current.getDayOfWeek()) && !JOURS_FERIES.contains(current)) {
                count++;
            }

            current = current.plusDays(1);
        }

        return count;
    }

    @Override
    public List<CalendarEventDTO> getAllCalendarEvents() {
        return demandeCongeRepository.findAllForCalendar();
    }

    @Override
    public DemandeRefusDetailsDTO getRefusDetails(Long demandeId) {
        return demandeCongeRepository.findRefusDetailsById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée ou non refusée"));
    }

    private Employe getEmployeByEmail(String email) {
        return employeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec email: " + email));
    }

    private boolean checkConflitDates(Long employeId, LocalDate dateDebut, LocalDate dateFin, Long excludeId) {
        List<DemandeConge> demandesExistantes = demandeCongeRepository.findByEmployeId(employeId);

        return demandesExistantes.stream()
                .filter(d -> excludeId == null || !d.getId().equals(excludeId))
                .filter(d -> "EN_ATTENTE".equals(d.getStatut()) || "APPROUVE".equals(d.getStatut()))
                .anyMatch(d -> !dateDebut.isAfter(d.getDateFin()) && !dateFin.isBefore(d.getDateDebut()));
    }

    private String getManagerEmailFromEmploye(Employe employe) {
        if (employe.getManager() != null) {
            String email = employe.getManager().getEmail();

            if (email != null && !email.isEmpty()) {
                return email;
            }
        }

        if (employe.getDepartement() != null && !employe.getDepartement().isEmpty()) {
            Optional<Manager> deptManager = managerRepository
                    .findByDepartement(employe.getDepartement())
                    .stream()
                    .findFirst();

            if (deptManager.isPresent() && deptManager.get().getEmail() != null) {
                return deptManager.get().getEmail();
            }
        }

        List<Manager> activeManagers = managerRepository.findManagersActifs();

        if (!activeManagers.isEmpty()) {
            return activeManagers.get(0).getEmail();
        }

        throw new BusinessException("Aucun manager trouvé pour cet employé");
    }

    private String getAdminRHEmail() {
        List<Employe> adminRH = employeRepository.findAllAdminRH();

        if (!adminRH.isEmpty()) {
            return adminRH.get(0).getEmail();
        }

        List<Employe> admins = employeRepository.findAll()
                .stream()
                .filter(e -> "ADMIN_RH".equals(e.getRole()))
                .collect(Collectors.toList());

        if (!admins.isEmpty()) {
            return admins.get(0).getEmail();
        }

        log.warn("Aucun administrateur RH trouvé, utilisation d'un email par défaut");
        return "admin@default.com";
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findAll() {
        return demandeCongeRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeCongeDTO> findAll(Pageable pageable) {
        Page<DemandeConge> page = demandeCongeRepository.findAll(pageable);

        List<DemandeCongeDTO> dtos = page.getContent()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DemandeCongeDTO> findById(Long id) {
        return demandeCongeRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByEmployeId(Long employeId) {
        return demandeCongeRepository.findByEmployeId(employeId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByManagerId(Long managerId) {
        return demandeCongeRepository.findByManagerId(managerId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByStatut(String statut) {
        return demandeCongeRepository.findByStatut(statut)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByType(String type) {
        return demandeCongeRepository.findByType(type)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findDemandesUrgentes() {
        return demandeCongeRepository.findDemandesUrgentes()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findUrgentesEnAttente() {
        return demandeCongeRepository.findDemandesUrgentes()
                .stream()
                .filter(d -> "EN_ATTENTE".equals(d.getStatut()))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findDemandesEnAttentePourManager(Long managerId) {
        return demandeCongeRepository.findDemandesEnAttentePourManager(managerId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findCongesEnCours() {
        return demandeCongeRepository.findCongesEnCours()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DemandeCongeDTO create(DemandeCongeDTO dto) {
        if (dto.getEmployeId() == null) {
            throw new BusinessException("L'ID de l'employé est obligatoire");
        }

        Employe employe = employeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

        if (checkConflitDates(employe.getId(), dto.getDateDebut(), dto.getDateFin(), null)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }

        DemandeConge demande = mapper.toEntity(dto);
        demande.setEmploye(employe);
        demande.soumettre();
        demande.setUrgente(Boolean.TRUE.equals(dto.getUrgente()));

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO modifier(Long id, DemandeCongeDTO dto) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être modifiées");
        }

        LocalDate newDebut = dto.getDateDebut() != null
                ? dto.getDateDebut()
                : demande.getDateDebut();

        LocalDate newFin = dto.getDateFin() != null
                ? dto.getDateFin()
                : demande.getDateFin();

        if (checkConflitDates(demande.getEmploye().getId(), newDebut, newFin, id)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }

        String nouveauType = dto.getType() != null
                ? dto.getType()
                : demande.getType();

        demande.modifier(
                newDebut,
                newFin,
                nouveauType,
                dto.getCommentaire()
        );

        demande.setUrgente(Boolean.TRUE.equals(dto.getUrgente()));

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO annuler(Long id) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        demande.annuler();

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO valider(Long id, Long managerId) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        if (demande.getEmploye().getManager() == null ||
                !demande.getEmploye().getManager().getId().equals(managerId)) {
            throw new BusinessException("Ce manager n'est pas responsable de cet employé");
        }

        demande.valider();
        demande.setManager(manager);

        if ("ANNUEL".equals(demande.getType())) {
            demande.getEmploye().deduireConges(demande.getJoursOuvres());
            employeRepository.save(demande.getEmploye());
        }

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO refuser(Long id, Long managerId, String motif) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        if (demande.getEmploye().getManager() == null ||
                !demande.getEmploye().getManager().getId().equals(managerId)) {
            throw new BusinessException("Ce manager n'est pas responsable de cet employé");
        }

        demande.refuserAvecMotif(motif);
        demande.setManager(manager);

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        demandeCongeRepository.delete(demande);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatut() {
        return demandeCongeRepository.countByStatut()
                .stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByType() {
        return demandeCongeRepository.countByType()
                .stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getStatsMensuelles(int annee) {
        return demandeCongeRepository.countByMois(annee)
                .stream()
                .collect(Collectors.toMap(arr -> (Integer) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConflitDates(Long employeId, LocalDate debut, LocalDate fin, Long demandeId) {
        return checkConflitDates(employeId, debut, fin, demandeId);
    }

    @Override
    public List<DemandeRefusManagerDTO> getDemandesRefuseesParManager() {
        return demandeCongeRepository.findDemandesRefuseesParManager();
    }

    // ========== CRÉATION EMPLOYÉ AVEC WORKFLOW CAMUNDA ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DemandeCongeDTO createForAuthenticatedUser(DemandeCongeDTO dto, String email) {
        Employe employe = getEmployeByEmail(email);
        dto.setEmployeId(employe.getId());

        if (dto.getDateDebut() == null || dto.getDateFin() == null) {
            throw new BusinessException("Les dates de début et de fin sont obligatoires");
        }

        if (dto.getDateDebut().isAfter(dto.getDateFin())) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin");
        }

        if (dto.getDateDebut().isBefore(LocalDate.now())) {
            throw new BusinessException("La date de début ne peut pas être dans le passé");
        }

        if (checkConflitDates(employe.getId(), dto.getDateDebut(), dto.getDateFin(), null)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }

        DemandeConge demande = mapper.toEntity(dto);
        demande.setEmploye(employe);
        demande.soumettre();
        demande.setUrgente(Boolean.TRUE.equals(dto.getUrgente()));

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        try {
            Map<String, Object> workflowVariables = new HashMap<>();

            workflowVariables.put("employeId", String.valueOf(employe.getId()));
            workflowVariables.put("montantConge", saved.getJoursOuvres());
            workflowVariables.put("nbJours", saved.getJoursOuvres());
            workflowVariables.put("demandeId", saved.getId());
            workflowVariables.put("typeConge", saved.getType());
            workflowVariables.put("dateDebut", saved.getDateDebut().toString());
            workflowVariables.put("dateFin", saved.getDateFin().toString());
            workflowVariables.put("managerEmail", getManagerEmailFromEmploye(employe));
            workflowVariables.put("adminEmail", getAdminRHEmail());
            workflowVariables.put("adminRHEmail", getAdminRHEmail());

            boolean urgente = Boolean.TRUE.equals(saved.getUrgente());

            workflowVariables.put("urgente", urgente);
            workflowVariables.put("urgent", urgente);
            workflowVariables.put("isUrgent", urgente);

            var processInstance = runtimeService.startProcessInstanceByKey(
                    "LeaveRequestProcess",
                    workflowVariables
            );

            saved.setProcessInstanceId(processInstance.getId());

            Task activeTask = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .active()
                    .singleResult();

            if (activeTask != null) {
                saved.setCurrentTaskId(activeTask.getId());
            }

            saved = demandeCongeRepository.saveAndFlush(saved);

            /*
             * Règle:
             * - Employé normal: reste chez manager.
             * - Manager/Admin/Admin RH: passage automatique vers RH.
             */
            workflowService.envoyerDirectementVersRhSiManagerOuAdmin(saved, employe);

            Long savedId = saved.getId();

            saved = demandeCongeRepository.findById(savedId)
                    .orElseThrow(() -> new BusinessException("Demande introuvable après création: " + savedId));

        } catch (Exception e) {
            log.error("Erreur workflow Camunda: {}", e.getMessage(), e);
            throw new BusinessException("Erreur technique lors du traitement de la demande: " + e.getMessage());
        }

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByEmployeEmail(String email) {
        Employe employe = getEmployeByEmail(email);

        return demandeCongeRepository.findByEmployeId(employe.getId())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SoldeCongesDTO getSoldeCongesByEmail(String email) {
        Employe employe = getEmployeByEmail(email);

        Integer total = employe.getSoldeConges() != null
                ? employe.getSoldeConges()
                : 25;

        int annee = LocalDate.now().getYear();

        int pris = demandeCongeRepository.sumJoursOuvresApprouvesAnnee(employe.getId(), annee);

        long enAttente = demandeCongeRepository.countByEmployeIdAndStatut(
                employe.getId(),
                "EN_ATTENTE"
        );

        return SoldeCongesDTO.builder()
                .total(total)
                .pris(pris)
                .restant(total - pris)
                .enAttente((int) enAttente)
                .build();
    }

    @Override
    public DemandeCongeDTO modifierForAuthenticatedUser(Long id, DemandeCongeDTO dto, String email) {
        Employe employe = getEmployeByEmail(email);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        if (!demande.getEmploye().getId().equals(employe.getId())) {
            throw new BusinessException("Vous ne pouvez pas modifier une demande qui ne vous appartient pas");
        }

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être modifiées");
        }

        LocalDate newDebut = dto.getDateDebut() != null
                ? dto.getDateDebut()
                : demande.getDateDebut();

        LocalDate newFin = dto.getDateFin() != null
                ? dto.getDateFin()
                : demande.getDateFin();

        if (newDebut.isBefore(LocalDate.now())) {
            throw new BusinessException("La date de début ne peut pas être dans le passé");
        }

        if (newDebut.isAfter(newFin)) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin");
        }

        if (checkConflitDates(employe.getId(), newDebut, newFin, id)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }

        String nouveauType = dto.getType() != null
                ? dto.getType()
                : demande.getType();

        Boolean nouvelleUrgence = Boolean.TRUE.equals(dto.getUrgente());

        demande.modifier(
                newDebut,
                newFin,
                nouveauType,
                dto.getCommentaire()
        );

        demande.setUrgente(nouvelleUrgence);

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO annulerForAuthenticatedUser(Long id, String email) {
        Employe employe = getEmployeByEmail(email);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        if (!demande.getEmploye().getId().equals(employe.getId())) {
            throw new BusinessException("Vous ne pouvez pas annuler une demande qui ne vous appartient pas");
        }

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être annulées");
        }

        /*
         * On annule seulement en base.
         * On ne supprime pas l'instance Camunda ici pour éviter rollback-only.
         */
        demande.annuler();

        DemandeConge saved = demandeCongeRepository.saveAndFlush(demande);

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> getCongesByEmployeIdForManager(Long employeId, String email) {
        Employe utilisateur = employeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if ("ADMIN_RH".equalsIgnoreCase(utilisateur.getRole())) {
            return demandeCongeRepository.findByEmployeId(employeId)
                    .stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
        }

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        if (employe.getManager() == null ||
                !employe.getManager().getId().equals(utilisateur.getId())) {
            throw new BusinessException("Cet employé n'appartient pas à votre équipe");
        }

        return demandeCongeRepository.findByEmployeId(employeId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalendarEventDTO> getCalendarEventsForManager(String managerEmail) {
        log.debug("Récupération des événements calendrier pour le manager : {}", managerEmail);

        Manager manager = managerRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", "email", managerEmail));

        List<Employe> equipe = employeRepository.findByManagerId(manager.getId());

        List<Long> employesIds = equipe.stream()
                .map(Employe::getId)
                .collect(Collectors.toList());

        if (employesIds.isEmpty()) {
            return List.of();
        }

        return demandeCongeRepository.findCalendarEventsForEmployes(employesIds);
    }
}