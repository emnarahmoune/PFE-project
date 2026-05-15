package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
import com.codeWithProject.ecom.service.*;
import com.codeWithProject.ecom.service.dto.*;
import com.codeWithProject.ecom.service.exception.*;
import com.codeWithProject.ecom.service.mapper.EmployeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeServiceImpl implements EmployeService {

    private final ManagerRepository managerRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeRepository employeRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final EvaluationRepository evaluationRepository;
    private final CompetenceRepository competenceRepository;

    private final FormationRecommendationAutoService formationRecommendationAutoService;
    private final KeycloakAdminService keycloakAdminService;
    private final EmployeMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleProvisioningService roleProvisioningService;

    // =========================
    // CREATE
    // =========================

    @Override
    public EmployeDTO create(EmployeDTO dto) {
        String role = roleProvisioningService.normalizeRole(dto.getRole());

        Employe employe;

        if ("MANAGER".equalsIgnoreCase(role)) {
            employe = new Manager();
        } else if ("ADMIN_RH".equalsIgnoreCase(role)) {
            employe = new AdministrateurRH();
        } else {
            employe = new Employe();
        }

        employe.setNom(dto.getNom());
        employe.setPrenom(dto.getPrenom());

        if (dto.getEmail() != null) {
            employe.setEmail(dto.getEmail().trim().toLowerCase());
        }

        employe.setTelephone(dto.getTelephone());
        employe.setPoste(dto.getPoste());
        employe.setDepartement(dto.getDepartement());
        employe.setAdresse(dto.getAdresse());
        employe.setMatricule(dto.getMatricule());
        employe.setSalaire(
        dto.getSalaire() != null
                ? dto.getSalaire()
                : null
);
        employe.setDateEmbauche(dto.getDateEmbauche());
        employe.setSoldeConges(dto.getSoldeConges());
        employe.setPhotoUrl(dto.getPhotoUrl());
        employe.setRole(role);

        String password = dto.getPassword() != null && !dto.getPassword().isBlank()
                ? dto.getPassword()
                : "default123";

        employe.setPassword(passwordEncoder.encode(password));

        if (employe.getActif() == null) {
            employe.setActif(true);
        }

        if (employe.getStatut() == null) {
            employe.setStatut("ACTIF");
        }

        applyManagerRules(employe, dto.getManagerId());

        Employe saved = employeRepository.saveAndFlush(employe);
        Long savedId = saved.getId();

        roleProvisioningService.provisionRole(saved);

        Employe refreshed = employeRepository.findById(savedId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", savedId));

        return mapper.toDto(refreshed);
    }

    // =========================
    // UPDATE
    // =========================

    @Override
    public EmployeDTO update(Long id, EmployeDTO dto) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        String oldRole = employe.getRole();

        if (dto.getNom() != null) {
            employe.setNom(dto.getNom());
        }

        if (dto.getPrenom() != null) {
            employe.setPrenom(dto.getPrenom());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            employe.setEmail(dto.getEmail().trim().toLowerCase());
        }

        if (dto.getPoste() != null) {
            employe.setPoste(dto.getPoste());
        }

        if (dto.getDepartement() != null) {
            employe.setDepartement(dto.getDepartement());
        }

        if (dto.getTelephone() != null) {
            employe.setTelephone(dto.getTelephone());
        }

        if (dto.getStatut() != null) {
            employe.setStatut(dto.getStatut());
        }

        if (dto.getPhotoUrl() != null && !dto.getPhotoUrl().isBlank()) {
            employe.setPhotoUrl(dto.getPhotoUrl());
        }

        if (dto.getMatricule() != null && !dto.getMatricule().isBlank()) {
            employe.setMatricule(dto.getMatricule());
        }

        if (dto.getDateEmbauche() != null) {
            employe.setDateEmbauche(dto.getDateEmbauche());
        }

        if (dto.getSalaire() != null) {
            employe.setSalaire(dto.getSalaire());
        }

        if (dto.getSoldeConges() != null) {
            employe.setSoldeConges(dto.getSoldeConges());
        }

        boolean roleChanged = false;

        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            String newRole = roleProvisioningService.normalizeRole(dto.getRole());
            employe.setRole(newRole);
            roleChanged = oldRole == null || !oldRole.equalsIgnoreCase(newRole);
        }

        applyManagerRules(employe, dto.getManagerId());

        Employe saved = employeRepository.saveAndFlush(employe);
        Long savedId = saved.getId();

        if (roleChanged) {
            roleProvisioningService.provisionRole(saved);

            Employe refreshed = employeRepository.findById(savedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employé", savedId));

            return mapper.toDto(refreshed);
        }

        return mapper.toDto(saved);
    }

    // =========================
    // PHOTO PROFIL
    // =========================

    @Override
    public String uploadPhotoProfilByEmail(String email, MultipartFile file) throws IOException {
        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Email vide");
        }

        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + email));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }

        String uploadDir = "uploads/profile-photos";
        Files.createDirectories(Paths.get(uploadDir));

        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "employe-" + employe.getId() + "-" + UUID.randomUUID() + extension;
        Path filePath = Paths.get(uploadDir, filename);

        Files.write(filePath, file.getBytes());

        String photoUrl = "/api/uploads/profile-photos/" + filename;

        employe.setPhotoUrl(photoUrl);
        employeRepository.saveAndFlush(employe);

        log.info(
                "PHOTO PROFIL SAUVÉE => employeId={}, email={}, photoUrl={}",
                employe.getId(),
                employe.getEmail(),
                photoUrl
        );

        return photoUrl;
    }

    @Override
    public void deletePhotoProfilByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Email vide");
        }

        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + email));

        employe.setPhotoUrl(null);
        employeRepository.saveAndFlush(employe);

        log.info(
                "PHOTO PROFIL SUPPRIMÉE => employeId={}, email={}",
                employe.getId(),
                employe.getEmail()
        );
    }

    // =========================
    // BASIC
    // =========================

    @Override
    public List<EmployeDTO> findAll() {
        return employeRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public Page<EmployeDTO> findAll(Pageable pageable) {
        return employeRepository.findAll(pageable)
                .map(mapper::toDto);
    }

    @Override
    public long count() {
        return employeRepository.count();
    }

    @Override
    public Optional<EmployeDTO> findById(Long id) {
        return employeRepository.findById(id)
                .map(mapper::toDto);
    }

   @Override
public Optional<EmployeDTO> findByEmail(String email) {
    if (email == null || email.isBlank()) {
        return Optional.empty();
    }

    return employeRepository.findByEmailIgnoreCase(email.trim())
            .map(employe -> {

                EmployeDTO dto = mapper.toDto(employe);

                Integer soldeTotal = employe.getSoldeConges() == null
                        ? 0
                        : employe.getSoldeConges();

                int joursPris = demandeCongeRepository
                        .findByEmploye_IdOrderByDateDemandeDesc(employe.getId())
                        .stream()
                        .filter(c ->
                                "APPROUVE".equalsIgnoreCase(String.valueOf(c.getStatut()))
                                        || "APPROUVÉ".equalsIgnoreCase(String.valueOf(c.getStatut()))
                                        || "ACCEPTE".equalsIgnoreCase(String.valueOf(c.getStatut()))
                                        || "ACCEPTÉ".equalsIgnoreCase(String.valueOf(c.getStatut()))
                        )
                        .mapToInt(c -> c.getJoursOuvres() == null ? 0 : c.getJoursOuvres())
                        .sum();

                int soldeRestant = Math.max(soldeTotal - joursPris, 0);

                dto.setSoldeConges(soldeRestant);

                return dto;
            });
}

    @Override
    public Optional<EmployeDTO> findByMatricule(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            return Optional.empty();
        }

        return employeRepository.findByMatricule(matricule.trim())
                .map(mapper::toDto);
    }

    // =========================
    // MANAGERS
    // =========================

    @Override
    public List<EmployeDTO> findAllManagers() {
        return employeRepository.findByRoleIgnoreCase("MANAGER")
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> getManagers() {
        return employeRepository.findByRoleIgnoreCase("MANAGER")
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public EmployeDTO updateManager(Long employeId, Long managerId) {
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        applyManagerRules(employe, managerId);

        return mapper.toDto(employeRepository.saveAndFlush(employe));
    }

    @Override
    public List<EmployeDTO> getEquipeByManagerEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }

        Employe managerEmploye = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Manager introuvable avec email: " + email));

        if (!"MANAGER".equalsIgnoreCase(managerEmploye.getRole())) {
            throw new RuntimeException("L'utilisateur connecté n'est pas un manager");
        }

        return employeRepository.findAll()
                .stream()
                .filter(e -> e.getManager() != null)
                .filter(e -> Objects.equals(e.getManager().getId(), managerEmploye.getId()))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public EmployeDTO getEmployeForManager(Long id, String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email manager obligatoire");
        }

        Employe managerEmploye = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Manager introuvable avec email: " + email));

        if (!"MANAGER".equalsIgnoreCase(managerEmploye.getRole())) {
            throw new RuntimeException("L'utilisateur connecté n'est pas un manager");
        }

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        if (employe.getManager() == null ||
                !Objects.equals(employe.getManager().getId(), managerEmploye.getId())) {
            throw new RuntimeException("Cet employé n'appartient pas à l'équipe de ce manager");
        }

        return mapper.toDto(employe);
    }

    // =========================
    // COMPETENCES
    // =========================

  @Override
public void addCompetence(Long userId, Long compId, int niveau) {
    Employe employe = employeRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Employé introuvable avec id: " + userId));

    Competence competence = competenceRepository.findById(compId)
            .orElseThrow(() -> new RuntimeException("Compétence introuvable avec id: " + compId));

    Optional<EmployeCompetence> existing =
            employeCompetenceRepository.findByEmploye_IdAndCompetence_Id(userId, compId);

    if (existing.isPresent()) {
        EmployeCompetence ec = existing.get();
        ec.setNiveau(convertToLevel(niveau));
        employeCompetenceRepository.save(ec);

        formationRecommendationAutoService.generateBoostRecommendationsForEmploye(employe);

        log.info(
                "Compétence mise à jour: employeId={}, competenceId={}, niveau={}",
                userId,
                compId,
                ec.getNiveau()
        );

        return;
    }

    EmployeCompetence ec = new EmployeCompetence();
    ec.setEmploye(employe);
    ec.setCompetence(competence);
    ec.setNiveau(convertToLevel(niveau));

    employeCompetenceRepository.save(ec);

    formationRecommendationAutoService.generateBoostRecommendationsForEmploye(employe);

    log.info(
            "Compétence ajoutée: employeId={}, competenceId={}, niveau={}",
            userId,
            compId,
            ec.getNiveau()
    );
}

   @Override
public void updateCompetence(Long userId, Long compId, int niveau) {
    Employe employe = employeRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Employé introuvable avec id: " + userId));

    EmployeCompetence ec = employeCompetenceRepository
            .findByEmploye_IdAndCompetence_Id(userId, compId)
            .orElseThrow(() -> new RuntimeException("Compétence employé introuvable"));

    ec.setNiveau(convertToLevel(niveau));
    employeCompetenceRepository.save(ec);

    formationRecommendationAutoService.generateBoostRecommendationsForEmploye(employe);
}

    @Override
    public List<EmployeCompetence> getCompetencesEntity(Long id) {
        return employeCompetenceRepository.findByEmploye_Id(id);
    }

    @Override
    public List<CompetenceEmployeDTO> getCompetencesByEmail(String email) {
        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));

        return employeCompetenceRepository.findByEmploye_Id(employe.getId())
                .stream()
                .map(ec -> {
                    Competence c = ec.getCompetence();

                    return CompetenceEmployeDTO.builder()
                            .id(ec.getId())
                            .competenceId(c != null ? c.getId() : null)
                            .nom(c != null ? c.getNom() : null)
                            .categorie(c != null ? c.getCategorie() : null)
                            .description(c != null ? c.getDescription() : null)
                            .niveau(ec.getNiveau())
                            .build();
                })
                .toList();
    }

    @Override
    public void updateCompetences(Long id, List<CompetenceDTO> dtos) {
        if (dtos == null) {
            return;
        }

        for (CompetenceDTO dto : dtos) {
            if (dto.getId() != null) {
                addCompetence(id, dto.getId(), 1);
            }
        }
    }

    // =========================
    // FORMATIONS / CONGES / EVALUATIONS
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<FormationEmployeDTO> getFormationsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }

        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));

        return getFormationsByEmployeId(employe.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationEmployeDTO> getFormationsByEmployeId(Long employeId) {
        if (employeId == null) {
            return List.of();
        }

        return employeFormationRepository.findByEmploye_Id(employeId)
                .stream()
                .filter(ef -> ef.getFormation() != null)
                .map(ef -> {
                    Formation formation = ef.getFormation();

                    return FormationEmployeDTO.builder()
                            .id(formation.getId())
                            .titre(formation.getTitre())
                            .domaine(formation.getDomaine())
                            .statut(ef.getStatut())
                            .progression(ef.getProgression() != null ? ef.getProgression() : 0)
                            .dateDebut(
                                    ef.getDateInscription() != null
                                            ? ef.getDateInscription().toLocalDate()
                                            : null
                            )
                            .dateFin(
                                    ef.getDateCompletion() != null
                                            ? ef.getDateCompletion().toLocalDate()
                                            : null
                            )
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueCongeDTO> getHistoriqueCongesByEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }

        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));

        return getCongesByEmployeId(employe.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueCongeDTO> getCongesByEmployeId(Long employeId) {
        if (employeId == null) {
            return List.of();
        }

        return demandeCongeRepository.findByEmploye_IdOrderByDateDemandeDesc(employeId)
                .stream()
                .map(c -> HistoriqueCongeDTO.builder()
                        .id(c.getId())
                        .dateDebut(c.getDateDebut())
                        .dateFin(c.getDateFin())
                        .type(c.getType())
                        .statut(c.getStatut())
                        .joursOuvres(c.getJoursOuvres())
                        .dateDemande(c.getDateDemande())
                        .dateDecision(c.getDateDecision())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationDTO> getEvaluationsByEmployeId(Long employeId) {
        if (employeId == null) {
            return List.of();
        }

        return evaluationRepository.findByEmploye_IdOrderByDateEvaluationDesc(employeId)
                .stream()
                .map(e -> EvaluationDTO.builder()
                        .id(e.getId())
                        .employeId(e.getEmploye() != null ? e.getEmploye().getId() : null)
                        .employeNom(e.getEmploye() != null ? e.getEmploye().getNom() : null)
                        .employePrenom(e.getEmploye() != null ? e.getEmploye().getPrenom() : null)
                        .dateEvaluation(e.getDateEvaluation())
                        .note(e.getNote())
                        .objectifsAtteints(e.getObjectifsAtteints())
                        .commentaire(e.getCommentaire())
                        .evaluateurId(e.getEvaluateur() != null ? e.getEvaluateur().getId() : null)
                        .evaluateurNom(buildFullName(e.getEvaluateur()))
                        .build())
                .toList();
    }

    // =========================
    // PROFIL CONNECTE
    // =========================

    @Override
    public EmployeDTO updateProfilByEmail(String email, UpdateProfilRequest request) {
        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));

        if (request.getTelephone() != null) {
            employe.setTelephone(request.getTelephone());
        }

        if (request.getAdresse() != null) {
            employe.setAdresse(request.getAdresse());
        }

        return mapper.toDto(employeRepository.save(employe));
    }

  @Override
@Transactional
public void changePassword(Jwt jwt, ChangePasswordRequest dto) {
    if (jwt == null) {
        throw new BusinessException("Utilisateur non authentifié");
    }

    if (dto == null) {
        throw new BusinessException("Données de mot de passe invalides");
    }

    if (dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
        throw new BusinessException("L'ancien mot de passe est obligatoire");
    }

    if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
        throw new BusinessException("Le nouveau mot de passe est obligatoire");
    }

    if (dto.getNewPassword().length() < 6) {
        throw new BusinessException("Le mot de passe doit contenir au moins 6 caractères");
    }

    if (dto.getOldPassword().equals(dto.getNewPassword())) {
        throw new BusinessException("Le nouveau mot de passe doit être différent de l'ancien");
    }

    String usernameOrEmail = extractEmail(jwt);

    if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
        throw new BusinessException("Email ou username Keycloak introuvable");
    }

    Employe employe = employeRepository.findByEmailIgnoreCase(usernameOrEmail.trim())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Employé introuvable : " + usernameOrEmail
            ));

    boolean oldPasswordOk = keycloakAdminService.verifyPassword(
            usernameOrEmail,
            dto.getOldPassword()
    );

    if (!oldPasswordOk) {
        throw new BusinessException("Ancien mot de passe incorrect");
    }

    String keycloakUserId = jwt.getSubject();

    if (keycloakUserId == null || keycloakUserId.isBlank()) {
        throw new BusinessException("Identifiant Keycloak introuvable");
    }

    keycloakAdminService.resetPassword(
            keycloakUserId,
            dto.getNewPassword()
    );

    /*
     * Important :
     * Keycloak est la source du mot de passe.
     * On évite de sauvegarder le nouveau mot de passe dans employes.password.
     *
     * Si ton ancien système utilise encore employes.password ailleurs,
     * tu peux temporairement garder la synchronisation en décommentant ces lignes :
     *
     * employe.setPassword(passwordEncoder.encode(dto.getNewPassword()));
     * employeRepository.saveAndFlush(employe);
     */

    log.info(
            "Mot de passe changé avec succès dans Keycloak pour employeId={}, email={}",
            employe.getId(),
            employe.getEmail()
    );
}

    @Override
    public EmployeDTO changeEmailByEmail(String email, String newEmail) {
        Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));

        employe.setEmail(newEmail.trim().toLowerCase());

        return mapper.toDto(employeRepository.save(employe));
    }

    @Override
    public Long getEmployeIdByEmail(String email) {
        return employeRepository.findByEmailIgnoreCase(email.trim())
                .map(Employe::getId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));
    }

    // =========================
    // STATUT / PROFIL
    // =========================

    @Override
    public EmployeDTO changerStatut(Long id, String nouveauStatut) {
        Employe emp = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        emp.setStatut(nouveauStatut);

        return mapper.toDto(employeRepository.save(emp));
    }

    @Override
    public EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement) {
        Employe emp = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        if (poste != null) {
            emp.setPoste(poste);
        }

        if (salaire != null) {
    emp.setSalaire(BigDecimal.valueOf(salaire));
}

        if (departement != null) {
            emp.setDepartement(departement);
        }

        return mapper.toDto(employeRepository.save(emp));
    }

    // =========================
    // DELETE
    // =========================

 @Override
@Transactional
public void delete(Long id) {

    Employe employe = employeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

    log.info("Désactivation employé : id={}, email={}, role={}",
            employe.getId(), employe.getEmail(), employe.getRole());

    /*
     * Soft delete :
     * On ne supprime rien en base.
     * On garde l'historique, les congés, formations, compétences, évaluations.
     */
    employe.setStatut("INACTIF");
    employe.setActif(false);

    /*
     * Sécurité :
     * Si l'employé désactivé est un manager,
     * on retire ce manager des employés qu'il gérait.
     * Mais on ne supprime aucun employé.
     */
    if ("MANAGER".equalsIgnoreCase(employe.getRole())) {
        List<Employe> equipes = employeRepository.findAll()
                .stream()
                .filter(e -> e.getManager() != null)
                .filter(e -> e.getManager().getId().equals(employe.getId()))
                .toList();

        for (Employe emp : equipes) {
            emp.setManager(null);
            employeRepository.save(emp);
        }
    }

    employeRepository.saveAndFlush(employe);

    log.info("Employé désactivé avec succès : id={}", id);
}
    // =========================
    // RECHERCHES SIMPLES
    // =========================

    @Override
    public List<EmployeDTO> findByDepartement(String departement) {
        return employeRepository.findAll()
                .stream()
                .filter(e -> departement != null && departement.equalsIgnoreCase(e.getDepartement()))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> findByStatut(String statut) {
        return employeRepository.findAll()
                .stream()
                .filter(e -> statut != null && statut.equalsIgnoreCase(e.getStatut()))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> findActifs() {
        return employeRepository.findAll()
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getActif()))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> findByManagerId(Long managerId) {
        return employeRepository.findAll()
                .stream()
                .filter(e -> e.getManager() != null && Objects.equals(e.getManager().getId(), managerId))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> findByServiceId(Long serviceId) {
        return employeRepository.findAll()
                .stream()
                .filter(e -> e.getService() != null && Objects.equals(e.getService().getId(), serviceId))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> findSoldeCongesFaible(Integer solde) {
        return employeRepository.findAll()
                .stream()
                .filter(e -> e.getSoldeConges() != null && e.getSoldeConges() <= solde)
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> findEmployesRecents(int limit) {
        return employeRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Employe::getId).reversed())
                .limit(limit)
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        String k = keyword.toLowerCase();

        return employeRepository.findAll()
                .stream()
                .filter(e ->
                        contains(e.getNom(), k)
                                || contains(e.getPrenom(), k)
                                || contains(e.getEmail(), k)
                                || contains(e.getPoste(), k)
                                || contains(e.getDepartement(), k)
                )
                .map(mapper::toDto)
                .toList();
    }

    // =========================
    // STATS
    // =========================

    @Override
    public Map<String, Long> countByDepartement() {
        Map<String, Long> result = new HashMap<>();

        for (Employe e : employeRepository.findAll()) {
            String dep = e.getDepartement() != null ? e.getDepartement() : "NON_DEFINI";
            result.put(dep, result.getOrDefault(dep, 0L) + 1);
        }

        return result;
    }

    @Override
    public Map<String, Long> countByStatut() {
        Map<String, Long> result = new HashMap<>();

        for (Employe e : employeRepository.findAll()) {
            String statut = e.getStatut() != null ? e.getStatut() : "NON_DEFINI";
            result.put(statut, result.getOrDefault(statut, 0L) + 1);
        }

        return result;
    }

 @Override
public Double calculerMasseSalariale() {
    return employeRepository.findAll()
            .stream()
            .map(Employe::getSalaire)
            .filter(Objects::nonNull)
            .mapToDouble(BigDecimal::doubleValue)
            .sum();
}

@Override
public Double calculerSalaireMoyen() {
    List<BigDecimal> salaires = employeRepository.findAll()
            .stream()
            .map(Employe::getSalaire)
            .filter(Objects::nonNull)
            .toList();

    if (salaires.isEmpty()) {
        return 0.0;
    }

    return salaires.stream()
            .mapToDouble(BigDecimal::doubleValue)
            .average()
            .orElse(0.0);
}

    @Override
    public TableauBordEmployeDTO getStatsTableauBord() {
        return new TableauBordEmployeDTO();
    }

   @Override
public SoldeCongesDTO getSoldeCongesByEmail(String email) {
    Employe employe = employeRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new RuntimeException("Employé introuvable avec email: " + email));

    Integer soldeTotal = employe.getSoldeConges() == null ? 0 : employe.getSoldeConges();

    int joursPris = demandeCongeRepository.findByEmploye_IdOrderByDateDemandeDesc(employe.getId())
            .stream()
            .filter(c ->
                    "APPROUVE".equalsIgnoreCase(String.valueOf(c.getStatut()))
                            || "APPROUVÉ".equalsIgnoreCase(String.valueOf(c.getStatut()))
                            || "ACCEPTE".equalsIgnoreCase(String.valueOf(c.getStatut()))
                            || "ACCEPTÉ".equalsIgnoreCase(String.valueOf(c.getStatut()))
            )
            .mapToInt(c -> c.getJoursOuvres() == null ? 0 : c.getJoursOuvres())
            .sum();

    int soldeRestant = Math.max(soldeTotal - joursPris, 0);

    SoldeCongesDTO dto = new SoldeCongesDTO();

    try {
        dto.getClass().getMethod("setEmployeId", Long.class).invoke(dto, employe.getId());
    } catch (Exception ignored) {}

    try {
        dto.getClass().getMethod("setSoldeConges", Integer.class).invoke(dto, soldeRestant);
    } catch (Exception ignored) {}

    try {
        dto.getClass().getMethod("setSoldeCongesRestants", Integer.class).invoke(dto, soldeRestant);
    } catch (Exception ignored) {}

    return dto;
}
    // =========================
    // HELPERS
    // =========================

    private String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        if (email == null || email.isBlank()) {
            email = jwt.getSubject();
        }

        return email != null ? email.trim().toLowerCase() : null;
    }

    private String buildFullName(Employe employe) {
        if (employe == null) {
            return null;
        }

        String prenom = employe.getPrenom() != null ? employe.getPrenom() : "";
        String nom = employe.getNom() != null ? employe.getNom() : "";

        String fullName = (prenom + " " + nom).trim();

        return fullName.isBlank() ? null : fullName;
    }

    private String convertToLevel(int niveau) {
        return switch (niveau) {
            case 1 -> "DEBUTANT";
            case 2 -> "INTERMEDIAIRE";
            case 3 -> "AVANCE";
            case 4 -> "EXPERT";
            default -> "DEBUTANT";
        };
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void applyManagerRules(Employe employe, Long managerId) {
        if (employe == null) {
            throw new BusinessException("Employé invalide");
        }

        if ("MANAGER".equalsIgnoreCase(employe.getRole())) {
            employe.setManager(null);
            return;
        }

        if (managerId == null) {
            employe.setManager(null);
            return;
        }

        if (employe.getId() != null && managerId.equals(employe.getId())) {
            throw new BusinessException("Un employé ne peut pas être son propre manager");
        }

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new BusinessException("L'utilisateur sélectionné n'a pas le rôle MANAGER");
        }

        employe.setManager(manager);
    }
}