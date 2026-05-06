package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.ManagerService;
import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.ManagerDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.ManagerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ManagerServiceImpl implements ManagerService {

    private final ManagerRepository managerRepository;
    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final ManagerMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findAll() {
        log.debug("Récupération de tous les managers");

        return managerRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagerDTO> findAll(Pageable pageable) {
        log.debug("Récupération des managers avec pagination");

        Page<Manager> page = managerRepository.findAll(pageable);

        List<ManagerDTO> dtos = page.getContent()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagerDTO> findById(Long id) {
        log.debug("Recherche de manager par ID: {}", id);

        return managerRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagerDTO> findByEmployeId(Long employeId) {
        log.debug("Recherche de manager par employeId: {}", employeId);

        return managerRepository.findById(employeId)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagerDTO> findByEmployeMatricule(String matricule) {
        log.debug("Recherche de manager par matricule: {}", matricule);

        return managerRepository.findByMatricule(matricule)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findByDepartement(String departement) {
        log.debug("Recherche de managers par département: {}", departement);

        return managerRepository.findByDepartement(departement)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersActifs() {
        log.debug("Recherche des managers actifs");

        return managerRepository.findManagersActifs()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersSansEquipe() {
        log.debug("Recherche des managers sans équipe");

        return managerRepository.findManagersSansEquipe()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersAvecDemandesEnAttente() {
        log.debug("Recherche des managers avec demandes en attente");

        return managerRepository.findManagersAvecDemandesEnAttente()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersAvecDemandesUrgentes() {
        log.debug("Recherche des managers avec demandes urgentes");

        return managerRepository.findManagersAvecDemandesUrgentes()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ManagerDTO create(ManagerDTO dto) {
        log.debug("Création d'un nouveau manager");

        if (dto.getEmployeId() == null) {
            throw new BusinessException("L'ID de l'employé est obligatoire pour créer un manager");
        }

        Employe employe = employeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

        if (managerRepository.findById(employe.getId()).isPresent()) {
            throw new BusinessException("Cet employé est déjà un manager");
        }

        Manager manager = new Manager();

        manager.setId(employe.getId());
        manager.setMatricule(employe.getMatricule());
        manager.setNom(employe.getNom());
        manager.setPrenom(employe.getPrenom());
        manager.setEmail(employe.getEmail());
        manager.setTelephone(employe.getTelephone());
        manager.setPassword(employe.getPassword());
        manager.setDateEmbauche(employe.getDateEmbauche());
        manager.setPoste(employe.getPoste());
        manager.setSalaire(employe.getSalaire());
        manager.setStatut(employe.getStatut());
        manager.setDepartement(dto.getDepartement() != null ? dto.getDepartement() : employe.getDepartement());
        manager.setSoldeConges(employe.getSoldeConges());
        manager.setActif(employe.getActif() != null ? employe.getActif() : true);
        manager.setDateCreation(employe.getDateCreation());
        manager.setDerniereConnexion(employe.getDerniereConnexion());
        manager.setNombreConnexions(employe.getNombreConnexions());
        manager.setTentativesEchec(employe.getTentativesEchec());
        manager.setCompteVerrouille(employe.getCompteVerrouille() != null ? employe.getCompteVerrouille() : false);
        manager.setDateVerrouillage(employe.getDateVerrouillage());

        manager.setRole("MANAGER");
        manager.setManager(null);

        manager.setService(employe.getService());
        manager.setDateNomination(dto.getDateNomination() != null ? dto.getDateNomination() : LocalDate.now());

        Manager saved = managerRepository.save(manager);

        log.info("Manager créé avec succès - ID: {}", saved.getId());

        return mapper.toDto(saved);
    }

    @Override
    public ManagerDTO update(Long id, ManagerDTO dto) {
        log.debug("Mise à jour du manager ID: {}", id);

        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", id));

        if (dto.getDepartement() != null) {
            manager.setDepartement(dto.getDepartement());
        }

        if (dto.getDateNomination() != null) {
            manager.setDateNomination(dto.getDateNomination());
        }

        if (dto.getActif() != null) {
            manager.setActif(dto.getActif());
        }

        if (dto.getNom() != null) {
            manager.setNom(dto.getNom());
        }

        if (dto.getPrenom() != null) {
            manager.setPrenom(dto.getPrenom());
        }

        if (dto.getEmail() != null) {
            manager.setEmail(dto.getEmail());
        }

        if (dto.getTelephone() != null) {
            manager.setTelephone(dto.getTelephone());
        }

        if (dto.getPoste() != null) {
            manager.setPoste(dto.getPoste());
        }

        if (dto.getSalaire() != null) {
            manager.setSalaire(dto.getSalaire());
        }

        if (dto.getStatut() != null) {
            manager.setStatut(dto.getStatut());
        }

        manager.setRole("MANAGER");
        manager.setManager(null);

        Manager saved = managerRepository.save(manager);

        log.info("Manager mis à jour avec succès - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public ManagerDTO activer(Long id) {
        log.debug("Activation du manager ID: {}", id);

        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", id));

        manager.activer();

        return mapper.toDto(managerRepository.save(manager));
    }

    @Override
    public ManagerDTO desactiver(Long id) {
        log.debug("Désactivation du manager ID: {}", id);

        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", id));

        manager.desactiver();

        return mapper.toDto(managerRepository.save(manager));
    }

    @Override
    public ManagerDTO ajouterEmploye(Long managerId, Long employeId) {
        log.debug("Ajout de l'employé {} à l'équipe du manager {}", employeId, managerId);

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        if ("MANAGER".equalsIgnoreCase(employe.getRole())) {
            throw new BusinessException("Un manager ne peut pas être ajouté comme employé dans l'équipe d'un autre manager");
        }

        manager.ajouterEmploye(employe);

        employeRepository.save(employe);

        Manager saved = managerRepository.save(manager);

        return mapper.toDto(saved);
    }

    @Override
    public ManagerDTO retirerEmploye(Long managerId, Long employeId) {
        log.debug("Retrait de l'employé {} de l'équipe du manager {}", employeId, managerId);

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        manager.retirerEmploye(employe);

        employeRepository.save(employe);

        Manager saved = managerRepository.save(manager);

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getManagersStats() {
        List<Map<String, Object>> stats = managerRepository.getManagersStats();

        if (stats == null || stats.isEmpty()) {
            return Map.of();
        }

        return stats.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByDepartement() {
        return managerRepository.countManagersByDepartement()
                .stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculerAncienneteMoyenne() {
        return managerRepository.calculateAncienneteMoyenne();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> search(String keyword) {
        log.debug("Recherche de managers avec mot-clé: {}", keyword);

        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        return managerRepository.searchManagers(keyword.trim())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersRecents(int limit) {
        return managerRepository.findManagersRecents()
                .stream()
                .limit(limit)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String genererRapportEquipe(Long managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        return manager.genererRapportEquipe();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getStatsManagers() {
        return managerRepository.getStatsManagers();
    }

       @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDTO> getCalendarEvents(String managerEmail) {
        if (managerEmail == null || managerEmail.isBlank()) {
            throw new BusinessException("Email manager introuvable");
        }

        Employe manager = employeRepository.findByEmailIgnoreCase(managerEmail.trim())
                .orElseThrow(() -> new BusinessException("Manager introuvable : " + managerEmail));

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new BusinessException("L'utilisateur connecté n'est pas un manager");
        }

        List<Employe> equipe = employeRepository.findAll()
                .stream()
                .filter(e -> e.getManager() != null)
                .filter(e -> e.getManager().getId() != null)
                .filter(e -> e.getManager().getId().equals(manager.getId()))
                .toList();

        List<Long> employeIds = equipe.stream()
                .map(Employe::getId)
                .toList();

        if (employeIds.isEmpty()) {
            return List.of();
        }

        return demandeCongeRepository.findAll()
                .stream()
                .filter(demande -> demande.getEmploye() != null)
                .filter(demande -> employeIds.contains(demande.getEmploye().getId()))
                .map(this::toCalendarEvent)
                .toList();
    }

   private CalendarEventDTO toCalendarEvent(DemandeConge demande) {
    CalendarEventDTO dto = new CalendarEventDTO();

    String statut = getStatutAsString(demande);
    String type = getTypeAsString(demande);

    dto.setId(demande.getId());
    dto.setTitle(buildCalendarTitle(demande));

    if (demande.getDateDebut() != null) {
        dto.setStart(demande.getDateDebut().atStartOfDay());
    }

    if (demande.getDateFin() != null) {
        dto.setEnd(demande.getDateFin().plusDays(1).atStartOfDay());
    }

    dto.setColor(resolveColorByStatut(statut));

    CalendarEventDTO.ExtendedProps props = new CalendarEventDTO.ExtendedProps();

    props.setDemandeId(demande.getId());
    props.setStatut(statut);
    props.setType(type);

    if (demande.getEmploye() != null) {
        props.setEmployeNom(
                demande.getEmploye().getNom() != null
                        ? demande.getEmploye().getNom()
                        : ""
        );

        props.setEmployePrenom(
                demande.getEmploye().getPrenom() != null
                        ? demande.getEmploye().getPrenom()
                        : ""
        );

        props.setEmployeEmail(
                demande.getEmploye().getEmail() != null
                        ? demande.getEmploye().getEmail()
                        : ""
        );
    } else {
        props.setEmployeNom("");
        props.setEmployePrenom("");
        props.setEmployeEmail("");
    }

    dto.setExtendedProps(props);

    return dto;
}
    private String buildCalendarTitle(DemandeConge demande) {
        String prenom = "";
        String nom = "";

        if (demande.getEmploye() != null) {
            prenom = demande.getEmploye().getPrenom() != null
                    ? demande.getEmploye().getPrenom()
                    : "";

            nom = demande.getEmploye().getNom() != null
                    ? demande.getEmploye().getNom()
                    : "";
        }

        String type = getTypeAsString(demande);
        String fullName = (prenom + " " + nom).trim();

        return fullName.isBlank() ? type : fullName + " - " + type;
    }

    private String getStatutAsString(DemandeConge demande) {
        if (demande == null || demande.getStatut() == null) {
            return "";
        }

        return String.valueOf(demande.getStatut());
    }

    private String getTypeAsString(DemandeConge demande) {
        if (demande == null || demande.getType() == null) {
            return "Congé";
        }

        return String.valueOf(demande.getType());
    }

    private String resolveColorByStatut(String statut) {
        if (statut == null) {
            return "#4361ee";
        }

        return switch (statut.toUpperCase()) {
            case "APPROUVE", "APPROUVEE", "APPROUVÉ", "APPROUVÉE" -> "#10b981";
            case "REFUSE", "REFUSEE", "REFUSÉ", "REFUSÉE", "REFUSE_MANAGER", "REFUSE_PAR_MANAGER" -> "#ef4444";
            case "EN_ATTENTE", "EN_ATTENTE_MANAGER", "EN_ATTENTE_RH", "EN_ATTENTE_ADMIN" -> "#f59e0b";
            default -> "#4361ee";
        };
    }
}
