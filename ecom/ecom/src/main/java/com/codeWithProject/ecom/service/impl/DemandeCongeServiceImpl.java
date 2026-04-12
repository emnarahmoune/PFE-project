package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.repository.UtilisateurRepository;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
import com.codeWithProject.ecom.service.dto.SoldeCongesDTO;
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
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DemandeCongeServiceImpl implements DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeCongeMapper mapper;

    // Services Camunda
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    private Employe getEmployeByEmail(String email) {
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé: " + email));
        Employe employe = user.getEmploye();
        if (employe == null) {
            throw new BusinessException("Cet utilisateur n'est pas associé à un employé");
        }
        return employe;
    }

    private boolean checkConflitDates(Long employeId, LocalDate dateDebut, LocalDate dateFin, Long excludeId) {
        List<DemandeConge> demandesExistantes = demandeCongeRepository.findByEmployeId(employeId);
        return demandesExistantes.stream()
                .filter(d -> excludeId == null || !d.getId().equals(excludeId))
                .filter(d -> "EN_ATTENTE".equals(d.getStatut()) || "APPROUVEE".equals(d.getStatut()))
                .anyMatch(d -> !dateDebut.isAfter(d.getDateFin()) && !dateFin.isBefore(d.getDateDebut()));
    }

    private int calculateJoursOuvres(LocalDate debut, LocalDate fin) {
        int jours = 0;
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            if (current.getDayOfWeek().getValue() < 6) {
                jours++;
            }
            current = current.plusDays(1);
        }
        return jours;
    }

    private String getManagerEmailFromEmploye(Employe employe) {
        if (employe.getManager() != null) {
            Manager manager = employe.getManager();
            String email = manager.getEmail();
            if (email != null && !email.isEmpty()) {
                log.debug("✅ Manager trouvé pour employé {}: {}", employe.getId(), email);
                return email;
            }
        }

        if (employe.getDepartement() != null && !employe.getDepartement().isEmpty()) {
            Optional<Manager> deptManager = managerRepository.findByDepartement(employe.getDepartement())
                    .stream().findFirst();
            if (deptManager.isPresent() && deptManager.get().getEmail() != null) {
                log.debug("✅ Manager du département {} trouvé: {}", employe.getDepartement(), deptManager.get().getEmail());
                return deptManager.get().getEmail();
            }
        }

        List<Manager> activeManagers = managerRepository.findManagersActifs();
        if (!activeManagers.isEmpty()) {
            String email = activeManagers.get(0).getEmail();
            log.warn("⚠️ Aucun manager direct pour l'employé {}, utilisation du premier manager actif: {}",
                    employe.getId(), email);
            return email;
        }

        throw new BusinessException("❌ Aucun manager trouvé pour cet employé");
    }

    private String getAdminRHEmail() {
        List<Utilisateur> adminRHUsers = utilisateurRepository.findByTypeUtilisateur("ADMIN_RH");

        if (!adminRHUsers.isEmpty()) {
            String email = adminRHUsers.get(0).getEmail();
            log.debug("✅ Admin RH trouvé: {}", email);
            return email;
        }

        List<Utilisateur> adminUsers = utilisateurRepository.findByTypeUtilisateur("ADMIN");
        if (!adminUsers.isEmpty()) {
            String email = adminUsers.get(0).getEmail();
            log.debug("✅ Admin trouvé (utilisé comme RH): {}", email);
            return email;
        }

        log.warn("⚠️ Aucun administrateur RH trouvé, utilisation d'un email par défaut");
        return "admin@default.com";
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findAll() {
        return demandeCongeRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeCongeDTO> findAll(Pageable pageable) {
        Page<DemandeConge> page = demandeCongeRepository.findAll(pageable);
        List<DemandeCongeDTO> dtos = page.getContent().stream().map(mapper::toDto).collect(Collectors.toList());
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
        return demandeCongeRepository.findByEmployeId(employeId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByManagerId(Long managerId) {
        return demandeCongeRepository.findByManagerId(managerId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByStatut(String statut) {
        return demandeCongeRepository.findByStatut(statut).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByType(String type) {
        return demandeCongeRepository.findByType(type).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findDemandesUrgentes() {
        return demandeCongeRepository.findDemandesUrgentes().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findUrgentesEnAttente() {
        return demandeCongeRepository.findUrgentesEnAttente().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findDemandesEnAttentePourManager(Long managerId) {
        return demandeCongeRepository.findDemandesEnAttentePourManager(managerId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findCongesEnCours() {
        return demandeCongeRepository.findCongesEnCours().stream().map(mapper::toDto).collect(Collectors.toList());
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
        demande.setJoursOuvres(calculateJoursOuvres(dto.getDateDebut(), dto.getDateFin()));
        demande.soumettre();
        DemandeConge saved = demandeCongeRepository.save(demande);
        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO modifier(Long id, DemandeCongeDTO dto) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));
        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être modifiées");
        }
        LocalDate newDebut = dto.getDateDebut() != null ? dto.getDateDebut() : demande.getDateDebut();
        LocalDate newFin = dto.getDateFin() != null ? dto.getDateFin() : demande.getDateFin();
        if (checkConflitDates(demande.getEmploye().getId(), newDebut, newFin, id)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }
        demande.modifier(newDebut, newFin, dto.getType() != null ? dto.getType() : demande.getType(), dto.getCommentaire());
        DemandeConge saved = demandeCongeRepository.save(demande);
        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO annuler(Long id) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));
        demande.annuler();
        return mapper.toDto(demandeCongeRepository.save(demande));
    }

    @Override
    public DemandeCongeDTO valider(Long id, Long managerId) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));
        if (demande.getEmploye().getManager() == null || !demande.getEmploye().getManager().getId().equals(managerId)) {
            throw new BusinessException("Ce manager n'est pas responsable de cet employé");
        }
        demande.valider();
        demande.setManager(manager);
        if ("ANNUEL".equals(demande.getType())) {
            demande.getEmploye().deduireConges(demande.getJoursOuvres());
            employeRepository.save(demande.getEmploye());
        }
        return mapper.toDto(demandeCongeRepository.save(demande));
    }

    @Override
    public DemandeCongeDTO refuser(Long id, Long managerId, String motif) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));
        if (demande.getEmploye().getManager() == null || !demande.getEmploye().getManager().getId().equals(managerId)) {
            throw new BusinessException("Ce manager n'est pas responsable de cet employé");
        }
        demande.refuserAvecMotif(motif);
        demande.setManager(manager);
        return mapper.toDto(demandeCongeRepository.save(demande));
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
        return demandeCongeRepository.countByStatut().stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByType() {
        return demandeCongeRepository.countByType().stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getStatsMensuelles(int annee) {
        return demandeCongeRepository.countByMois(annee).stream()
                .collect(Collectors.toMap(arr -> (Integer) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConflitDates(Long employeId, LocalDate debut, LocalDate fin, Long demandeId) {
        return checkConflitDates(employeId, debut, fin, demandeId);
    }

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
        if ("ANNUEL".equals(dto.getType())) {
            int joursDemandes = calculateJoursOuvres(dto.getDateDebut(), dto.getDateFin());
            if (employe.getSoldeConges() < joursDemandes) {
                throw new BusinessException(String.format("Solde insuffisant. Disponible: %d, Demandé: %d",
                        employe.getSoldeConges(), joursDemandes));
            }
        }
        if (checkConflitDates(employe.getId(), dto.getDateDebut(), dto.getDateFin(), null)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }

        DemandeConge demande = mapper.toEntity(dto);
        demande.setEmploye(employe);
        demande.setJoursOuvres(calculateJoursOuvres(dto.getDateDebut(), dto.getDateFin()));
        demande.soumettre();
        DemandeConge saved = demandeCongeRepository.save(demande);

        try {
            Map<String, Object> workflowVariables = new HashMap<>();
            workflowVariables.put("employeId", String.valueOf(employe.getId()));
            workflowVariables.put("montantConge", saved.getJoursOuvres());
            workflowVariables.put("nbJours", saved.getJoursOuvres());
            workflowVariables.put("demandeId", saved.getId());
            workflowVariables.put("typeConge", saved.getType());
            workflowVariables.put("dateDebut", saved.getDateDebut().toString());
            workflowVariables.put("dateFin", saved.getDateFin().toString());

            String managerEmail = getManagerEmailFromEmploye(employe);
            workflowVariables.put("managerEmail", managerEmail);

            String adminEmail = getAdminRHEmail();
            workflowVariables.put("adminEmail", adminEmail);

            log.info("🚀 Démarrage workflow Camunda - Manager: {}, Admin RH: {}", managerEmail, adminEmail);

            var processInstance = runtimeService.startProcessInstanceByKey(
                    "Process_13wxvdy",
                    workflowVariables
            );

            saved.setProcessInstanceId(processInstance.getId());

            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .list();

            if (!tasks.isEmpty()) {
                saved.setCurrentTaskId(tasks.get(0).getId());
                log.info("📋 Tâche créée: '{}' pour assignee: '{}'", tasks.get(0).getName(), tasks.get(0).getAssignee());
            } else {
                log.warn("⚠️ Aucune tâche créée pour l'instance {}", processInstance.getId());
            }

            DemandeConge finalSaved = demandeCongeRepository.save(saved);
            log.info("✅ Demande créée avec succès - ID: {}, ProcessInstanceId: {}",
                    finalSaved.getId(), finalSaved.getProcessInstanceId());

        } catch (Exception e) {
            log.error("❌ Erreur lors du démarrage du workflow Camunda: {}", e.getMessage(), e);
            throw new BusinessException("Erreur technique lors du traitement de la demande: " + e.getMessage());
        }

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByEmployeEmail(String email) {
        Employe employe = getEmployeByEmail(email);
        return demandeCongeRepository.findByEmployeId(employe.getId()).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SoldeCongesDTO getSoldeCongesByEmail(String email) {
        Employe employe = getEmployeByEmail(email);
        Integer total = employe.getSoldeConges() != null ? employe.getSoldeConges() : 25;
        int annee = LocalDate.now().getYear();
        long pris = demandeCongeRepository.countCongesPrisAnnee(employe.getId(), annee);
        Integer restant = total - (int) pris;
        long enAttente = demandeCongeRepository.countByEmployeIdAndStatut(employe.getId(), "EN_ATTENTE");
        return SoldeCongesDTO.builder().total(total).pris((int) pris).restant(restant).enAttente((int) enAttente).build();
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
        LocalDate newDebut = dto.getDateDebut() != null ? dto.getDateDebut() : demande.getDateDebut();
        LocalDate newFin = dto.getDateFin() != null ? dto.getDateFin() : demande.getDateFin();
        if (checkConflitDates(employe.getId(), newDebut, newFin, id)) {
            throw new BusinessException("Une demande de congé existe déjà sur cette période");
        }
        demande.modifier(newDebut, newFin, dto.getType() != null ? dto.getType() : demande.getType(), dto.getCommentaire());
        demande.setJoursOuvres(calculateJoursOuvres(demande.getDateDebut(), demande.getDateFin()));
        return mapper.toDto(demandeCongeRepository.save(demande));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void supprimerInstanceCamunda(String processInstanceId) {
        if (processInstanceId != null) {
            try {
                runtimeService.deleteProcessInstance(processInstanceId, "Annulé par l'utilisateur");
                log.info("🗑️ Instance Camunda {} supprimée", processInstanceId);
            } catch (Exception e) {
                log.warn("⚠️ Instance Camunda introuvable ou déjà supprimée: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
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

        supprimerInstanceCamunda(demande.getProcessInstanceId());

        demande.annuler();
        DemandeConge saved = demandeCongeRepository.save(demande);
        return mapper.toDto(saved);
    }
}