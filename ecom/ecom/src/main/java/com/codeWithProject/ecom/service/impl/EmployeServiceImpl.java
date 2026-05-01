package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Competence;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeCompetence;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.CompetenceRepository;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeCompetenceRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.FormationRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.repository.ServiceRepository;
import com.codeWithProject.ecom.service.AuditService;
import com.codeWithProject.ecom.service.EmployeService;
import com.codeWithProject.ecom.service.KeycloakPasswordService;
import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import com.codeWithProject.ecom.service.dto.CompetenceDTO;
import com.codeWithProject.ecom.service.dto.CompetenceEmployeDTO;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import com.codeWithProject.ecom.service.dto.FormationEmployeDTO;
import com.codeWithProject.ecom.service.dto.HistoriqueCongeDTO;
import com.codeWithProject.ecom.service.dto.SoldeCongesDTO;
import com.codeWithProject.ecom.service.dto.TableauBordEmployeDTO;
import com.codeWithProject.ecom.service.dto.UpdateProfilRequest;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.EmployeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeWithProject.ecom.service.KeycloakPasswordService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeServiceImpl implements EmployeService {

    private final EmployeRepository employeRepository;
    private final ServiceRepository serviceRepository;
    private final ManagerRepository managerRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final FormationRepository formationRepository;
    private final EmployeMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final CompetenceRepository competenceRepository;
    private final AuditService auditService;
    private final KeycloakPasswordService keycloakPasswordService;

    // ===== MÉTHODES DE BASE =====

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findAll() {
        return employeRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeDTO> findAll(Pageable pageable) {
        Page<Employe> page = employeRepository.findAll(pageable);

        List<EmployeDTO> dtos = page.getContent()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return employeRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeDTO> findById(Long id) {
        return employeRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeDTO> findByMatricule(String matricule) {
        return employeRepository.findByMatricule(matricule).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByDepartement(String departement) {
        return employeRepository.findByDepartement(departement)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByStatut(String statut) {
        return employeRepository.findByStatut(statut)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByManagerId(Long managerId) {
        return employeRepository.findByManagerId(managerId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByServiceId(Long serviceId) {
        return employeRepository.findByServiceId(serviceId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findActifs() {
        return employeRepository.findAllActifs()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findSoldeCongesFaible(Integer seuil) {
        return employeRepository.findBySoldeCongesLessThan(seuil)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ===== CRUD =====

    @Override
    public EmployeDTO create(EmployeDTO dto) {
        log.debug("Création d'un employé: {}", dto.getMatricule());

        if (dto.getMatricule() == null || dto.getMatricule().trim().isEmpty()) {
            throw new BusinessException("Le matricule est obligatoire");
        }

        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new BusinessException("Le nom est obligatoire");
        }

        if (dto.getPrenom() == null || dto.getPrenom().trim().isEmpty()) {
            throw new BusinessException("Le prénom est obligatoire");
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BusinessException("L'email est obligatoire");
        }

        if (dto.getDateEmbauche() == null) {
            throw new BusinessException("La date d'embauche est obligatoire");
        }

        if (dto.getSalaire() == null) {
            throw new BusinessException("Le salaire est obligatoire");
        }

        if (employeRepository.existsByMatricule(dto.getMatricule())) {
            throw new BusinessException("Un employé avec ce matricule existe déjà");
        }

        if (employeRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BusinessException("Un employé avec cet email existe déjà");
        }

        Employe employe = mapper.toEntity(dto);

        if (dto.getServiceId() != null) {
            com.codeWithProject.ecom.entity.Service service = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", dto.getServiceId()));
            employe.setService(service);
        }

        if (dto.getManagerId() != null) {
            Manager manager = managerRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", dto.getManagerId()));
            employe.setManager(manager);
        } else if (dto.getManagerEmail() != null && !dto.getManagerEmail().isBlank()) {
            Manager manager = managerRepository.findByEmail(dto.getManagerEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager avec email " + dto.getManagerEmail()));
            employe.setManager(manager);
        }

        String rawPassword = dto.getPassword() != null ? dto.getPassword() : "default123";
        employe.setPassword(passwordEncoder.encode(rawPassword));

        String role = dto.getRole() != null ? dto.getRole() : "user";
        employe.setRole(role);
        employe.setTypeEmploye(determineTypeFromRole(role));

        if (employe.getActif() == null) {
            employe.setActif(true);
        }

        if (employe.getCompteVerrouille() == null) {
            employe.setCompteVerrouille(false);
        }

        if (employe.getNombreConnexions() == null) {
            employe.setNombreConnexions(0);
        }

        if (employe.getTentativesEchec() == null) {
            employe.setTentativesEchec(0);
        }

        if (employe.getSoldeConges() == null) {
            employe.setSoldeConges(25);
        }

        if (employe.getStatut() == null) {
            employe.setStatut("ACTIF");
        }

        Employe saved = employeRepository.save(employe);

        log.info("Employé créé avec succès - ID: {}, rôle: {}", saved.getId(), role);

        return mapper.toDto(saved);
    }

    private String determineTypeFromRole(String role) {
        if ("admin_rh".equalsIgnoreCase(role) || "ADMIN_RH".equalsIgnoreCase(role)) {
            return Employe.TYPE_ADMIN_RH;
        } else if ("manager".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            return Employe.TYPE_MANAGER;
        } else {
            return Employe.TYPE_EMPLOYE;
        }
    }

    @Override
    public EmployeDTO update(Long id, EmployeDTO dto) {
        Employe actor = getCurrentAuthenticatedEmploye();

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        String before = buildEmployeSnapshot(employe);

        if (dto.getNom() != null) {
            employe.setNom(dto.getNom());
        }

        if (dto.getPrenom() != null) {
            employe.setPrenom(dto.getPrenom());
        }

        if (dto.getEmail() != null) {
            employe.setEmail(dto.getEmail());
        }

        if (dto.getTelephone() != null) {
            employe.setTelephone(dto.getTelephone());
        }

        if (dto.getPoste() != null) {
            employe.setPoste(dto.getPoste());
        }

        if (dto.getDepartement() != null) {
            employe.setDepartement(dto.getDepartement());
        }

        if (dto.getRole() != null) {
            employe.setRole(dto.getRole());
            employe.setTypeEmploye(determineTypeFromRole(dto.getRole()));
        }

        if (dto.getStatut() != null) {
            employe.setStatut(dto.getStatut());

            if ("ACTIF".equalsIgnoreCase(dto.getStatut())) {
                employe.setActif(true);
            } else if ("INACTIF".equalsIgnoreCase(dto.getStatut())) {
                employe.setActif(false);
            }
        }

        if (dto.getActif() != null) {
            employe.setActif(dto.getActif());
        }

        Employe saved = employeRepository.save(employe);

        String after = buildEmployeSnapshot(saved);

        auditService.logAction(
                "MODIFICATION",
                actor.getId(),
                actor.getEmail(),
                "EMPLOYE",
                saved.getId(),
                saved.getEmail(),
                "Avant: " + before + " | Après: " + after
        );

        return mapper.toDto(saved);
    }

    @Override
    public EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        if (poste != null) {
            employe.setPoste(poste);
        }

        if (salaire != null) {
            employe.setSalaire(salaire);
        }

        if (departement != null) {
            employe.setDepartement(departement);
        }

        return mapper.toDto(employeRepository.save(employe));
    }

    @Override
    public EmployeDTO changerStatut(Long id, String nouveauStatut) {
        Employe actor = getCurrentAuthenticatedEmploye();

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        if (actor.getId().equals(employe.getId())) {
            throw new BusinessException("Vous ne pouvez pas changer votre propre statut.");
        }

        if ("ADMIN_RH".equalsIgnoreCase(employe.getRole()) || "admin_rh".equalsIgnoreCase(employe.getRole())) {
            throw new BusinessException("Vous ne pouvez pas désactiver un administrateur RH.");
        }

        String ancienStatut = employe.getStatut();

        employe.setStatut(nouveauStatut);

        if ("ACTIF".equalsIgnoreCase(nouveauStatut)) {
            employe.setActif(true);
        } else if ("INACTIF".equalsIgnoreCase(nouveauStatut)) {
            employe.setActif(false);
        }

        Employe saved = employeRepository.save(employe);

        auditService.logAction(
                "CHANGEMENT_STATUT",
                actor.getId(),
                actor.getEmail(),
                "EMPLOYE",
                saved.getId(),
                saved.getEmail(),
                "Ancien statut: " + ancienStatut + " | Nouveau statut: " + nouveauStatut
        );

        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        Employe actor = getCurrentAuthenticatedEmploye();

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        if (actor.getId().equals(employe.getId())) {
            throw new BusinessException("Vous ne pouvez pas vous désactiver vous-même.");
        }

        if ("ADMIN_RH".equalsIgnoreCase(employe.getRole()) || "admin_rh".equalsIgnoreCase(employe.getRole())) {
            throw new BusinessException("Vous ne pouvez pas désactiver un administrateur RH.");
        }

        String before = buildEmployeSnapshot(employe);

        employe.setStatut("INACTIF");
        employe.setActif(false);

        Employe saved = employeRepository.save(employe);

        String after = buildEmployeSnapshot(saved);

        auditService.logAction(
                "DESACTIVATION",
                actor.getId(),
                actor.getEmail(),
                "EMPLOYE",
                saved.getId(),
                saved.getEmail(),
                "Avant: " + before + " | Après: " + after
        );
    }

    // ===== STATISTIQUES =====

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByDepartement() {
        return employeRepository.countByDepartement()
                .stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatut() {
        return employeRepository.countByStatut()
                .stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculerMasseSalariale() {
        Double result = employeRepository.sommeSalaires();
        return result != null ? result : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculerSalaireMoyen() {
        Double result = employeRepository.salaireMoyen();
        return result != null ? result : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findEmployesRecents(int limit) {
        return employeRepository.findEmployesRecents()
                .stream()
                .limit(limit)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        return employeRepository.searchEmployes(keyword.trim())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TableauBordEmployeDTO getStatsTableauBord() {
        List<Map<String, Object>> stats = employeRepository.getStatsTableauBord();

        if (stats == null || stats.isEmpty()) {
            return TableauBordEmployeDTO.builder()
                    .total(0L)
                    .actifs(0L)
                    .inactifs(0L)
                    .enConge(0L)
                    .salaireMoyen(0.0)
                    .masseSalariale(0.0)
                    .soldeCongesMoyen(0.0)
                    .build();
        }

        Map<String, Object> stat = stats.get(0);

        return TableauBordEmployeDTO.builder()
                .total(toLong(stat.get("total")))
                .actifs(toLong(stat.get("actifs")))
                .inactifs(toLong(stat.get("inactifs")))
                .enConge(toLong(stat.get("enConge")))
                .salaireMoyen(toDouble(stat.get("salaireMoyen")))
                .masseSalariale(toDouble(stat.get("masseSalariale")))
                .soldeCongesMoyen(toDouble(stat.get("soldeCongesMoyen")))
                .build();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(value.toString());
    }

    // ===== MÉTHODES POUR L'UTILISATEUR AUTHENTIFIÉ =====

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeDTO> findByEmail(String email) {
        return employeRepository.findByEmail(email).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SoldeCongesDTO getSoldeCongesByEmail(String email) {
        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        Integer total = employe.getSoldeConges() != null ? employe.getSoldeConges() : 25;
        int annee = LocalDate.now().getYear();
        long pris = demandeCongeRepository.countCongesPrisAnnee(employe.getId(), annee);
        Integer restant = total - (int) pris;
        long enAttente = demandeCongeRepository.countByEmployeIdAndStatut(employe.getId(), "EN_ATTENTE");

        return SoldeCongesDTO.builder()
                .total(total)
                .pris((int) pris)
                .restant(restant)
                .enAttente((int) enAttente)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationEmployeDTO> getFormationsByEmail(String email) {
        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return formationRepository.findFormationsByEmployeId(employe.getId())
                .stream()
                .map(formation -> FormationEmployeDTO.builder()
                        .id(formation.getId())
                        .titre(formation.getTitre())
                        .domaine(formation.getDomaine())
                        .statut("INSCRIT")
                        .progression(0)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueCongeDTO> getHistoriqueCongesByEmail(String email) {
        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return demandeCongeRepository.findByEmployeId(employe.getId())
                .stream()
                .map(demande -> HistoriqueCongeDTO.builder()
                        .id(demande.getId())
                        .dateDebut(demande.getDateDebut())
                        .dateFin(demande.getDateFin())
                        .type(demande.getType())
                        .statut(demande.getStatut())
                        .joursOuvres(demande.getJoursOuvres())
                        .dateDemande(demande.getDateDemande())
                        .dateDecision(demande.getDateDecision())
                        .build())
                .collect(Collectors.toList());
    }

   @Override
public EmployeDTO updateProfilByEmail(String email, UpdateProfilRequest request) {
    Employe employe = employeRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

    if (request.getTelephone() != null) {
        employe.setTelephone(request.getTelephone());
    }

    if (request.getAdresse() != null) {
        employe.setAdresse(request.getAdresse());
    }

    if (request.getPoste() != null) {
        employe.setPoste(request.getPoste());
    }

    if (request.getDepartement() != null) {
        employe.setDepartement(request.getDepartement());
    }

    return mapper.toDto(employeRepository.save(employe));
}
@Override
public void changePasswordByEmail(String email, ChangePasswordRequest request) {
    employeRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

    if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
        throw new BusinessException("Ancien mot de passe obligatoire");
    }

    if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
        throw new BusinessException("Nouveau mot de passe obligatoire");
    }

    if (request.getNewPassword().length() < 8) {
        throw new BusinessException("Le nouveau mot de passe doit contenir au moins 8 caractères");
    }

    keycloakPasswordService.changePassword(
            email,
            request.getOldPassword(),
            request.getNewPassword()
    );
}
    @Override
    public EmployeDTO changeEmailByEmail(String email, String newEmail) {
        if (employeRepository.findByEmail(newEmail).isPresent()) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        employe.setEmail(newEmail);

        return mapper.toDto(employeRepository.save(employe));
    }

    // ===== MÉTHODES POUR MANAGER =====

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findAllManagers() {
        return employeRepository.findByRole("MANAGER")
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getEquipeByManagerEmail(String managerEmail) {
        Employe manager = employeRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager non trouvé avec email: " + managerEmail));

        return employeRepository.findByManagerId(manager.getId())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeDTO getEmployeForManager(Long employeId, String managerEmail) {
        Employe manager = employeRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager non trouvé"));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        if (employe.getManager() == null || !employe.getManager().getId().equals(manager.getId())) {
            throw new BusinessException("Cet employé n'appartient pas à votre équipe");
        }

        return mapper.toDto(employe);
    }

    @Override
    public EmployeDTO updateManager(Long employeId, Long managerId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        if (managerId == null) {
            employe.setManager(null);
        } else {
            Manager manager = managerRepository.findById(managerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));
            employe.setManager(manager);
        }

        return mapper.toDto(employeRepository.save(employe));
    }

    // ===== COMPÉTENCES =====

    private String convertToLevel(int niveau) {
        return switch (niveau) {
            case 1 -> "DEBUTANT";
            case 2 -> "INTERMEDIAIRE";
            case 3 -> "AVANCE";
            case 4, 5 -> "EXPERT";
            default -> "DEBUTANT";
        };
    }

    @Override
    public void addCompetence(Long userId, Long compId, int niveau) {
        boolean exists = employeCompetenceRepository.existsByEmployeIdAndCompetenceId(userId, compId);

        if (exists) {
            throw new BusinessException("Compétence déjà ajoutée");
        }

        Employe emp = employeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", userId));

        Competence comp = competenceRepository.findById(compId)
                .orElseThrow(() -> new ResourceNotFoundException("Compétence", compId));

        EmployeCompetence ec = new EmployeCompetence();
        ec.setEmploye(emp);
        ec.setCompetence(comp);
        ec.setNiveau(convertToLevel(niveau));

        employeCompetenceRepository.save(ec);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetenceEmployeDTO> getCompetencesByEmail(String email) {
        Optional<Employe> emp = employeRepository.findByEmail(email);

        if (emp.isEmpty()) {
            return new ArrayList<>();
        }

        List<EmployeCompetence> list = employeCompetenceRepository.findByEmployeId(emp.get().getId());

        if (list == null) {
            return new ArrayList<>();
        }

        return list.stream()
                .map(c -> {
                    CompetenceEmployeDTO dto = new CompetenceEmployeDTO();
                    dto.setNom(c.getCompetence().getNom());
                    dto.setNiveau(c.getNiveau());
                    return dto;
                })
                .toList();
    }

    @Override
    public void updateCompetences(Long employeId, List<CompetenceDTO> dtos) {
        for (CompetenceDTO dto : dtos) {
            EmployeCompetence ec = employeCompetenceRepository.findByEmployeAndCompetence(
                    employeId,
                    dto.getCompetenceId()
            );

            if (ec != null) {
                ec.setNiveau(convertToLevel(dto.getNiveau()));
                employeCompetenceRepository.save(ec);
            }
        }
    }

    @Override
    public Long getEmployeIdByEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase();

        return employeRepository.findByEmail(normalizedEmail)
                .map(Employe::getId)
                .orElse(null);
    }

    @Override
    public void updateCompetence(Long userId, Long compId, int niveau) {
        EmployeCompetence ec = employeCompetenceRepository.findByEmployeAndCompetence(userId, compId);

        if (ec != null) {
            ec.setNiveau(convertToLevel(niveau));
            employeCompetenceRepository.save(ec);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeCompetence> getCompetencesEntity(Long id) {
        return employeCompetenceRepository.findByEmployeId(id);
    }

    // ===== FORMATIONS =====

    @Override
    @Transactional(readOnly = true)
    public List<FormationEmployeDTO> getFormationsByEmployeId(Long employeId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        if (employe.getEmployeFormations() == null) {
            return List.of();
        }

        return employe.getEmployeFormations()
                .stream()
                .filter(ef -> ef.getFormation() != null)
                .map(ef -> FormationEmployeDTO.builder()
                        .id(ef.getFormation().getId())
                        .titre(ef.getFormation().getTitre())
                        .progression(ef.getProgression() != null ? ef.getProgression() : 0)
                        .statut(ef.getStatut())
                        .build())
                .toList();
    }

    // ===== HELPERS AUDIT =====

  private Employe getCurrentAuthenticatedEmploye() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
        throw new BusinessException("Utilisateur non authentifié");
    }

    String email = null;

    Object principal = authentication.getPrincipal();

    if (principal instanceof Jwt jwt) {
        email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
    }

    if (email == null || email.isBlank()) {
        email = authentication.getName();
    }

    final String finalEmail = email;

    return employeRepository.findByEmail(finalEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable: " + finalEmail));
}
    private String buildEmployeSnapshot(Employe employe) {
        return "{"
                + "nom='" + employe.getNom() + '\''
                + ", prenom='" + employe.getPrenom() + '\''
                + ", email='" + employe.getEmail() + '\''
                + ", telephone='" + employe.getTelephone() + '\''
                + ", poste='" + employe.getPoste() + '\''
                + ", departement='" + employe.getDepartement() + '\''
                + ", role='" + employe.getRole() + '\''
                + ", statut='" + employe.getStatut() + '\''
                + ", actif=" + employe.getActif()
                + '}';
    }
}