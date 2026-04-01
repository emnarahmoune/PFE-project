package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.entity.Utilisateur;
import com.codeWithProject.ecom.repository.*;
import com.codeWithProject.ecom.service.EmployeService;
import com.codeWithProject.ecom.service.dto.*;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.EmployeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class EmployeServiceImpl implements EmployeService {

    private final EmployeRepository employeRepository;
    private final ServiceRepository serviceRepository;
    private final ManagerRepository managerRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final FormationRepository formationRepository;
    private final EmployeMapper mapper;
    private final PasswordEncoder passwordEncoder;

    // ===== MÉTHODES EXISTANTES =====

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findAll() {
        log.debug("Récupération de tous les employés");
        return employeRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeDTO> findAll(Pageable pageable) {
        log.debug("Récupération des employés avec pagination");
        Page<Employe> page = employeRepository.findAll(pageable);
        List<EmployeDTO> dtos = page.getContent().stream()
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
        return employeRepository.findByDepartement(departement).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByStatut(String statut) {
        return employeRepository.findByStatut(statut).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByManagerId(Long managerId) {
        return employeRepository.findByManagerId(managerId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findByServiceId(Long serviceId) {
        return employeRepository.findByServiceId(serviceId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findActifs() {
        return employeRepository.findAllActifs().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeDTO> findSoldeCongesFaible(Integer seuil) {
        return employeRepository.findBySoldeCongesLessThan(seuil).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeDTO create(EmployeDTO dto) {
        log.debug("Création d'un employé: {}", dto.getMatricule());

        // Validation
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

        // Vérifier unicité
        if (employeRepository.existsByMatricule(dto.getMatricule())) {
            throw new BusinessException("Un employé avec ce matricule existe déjà");
        }
        if (utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Un utilisateur avec cet email existe déjà");
        }

        // Créer l'employé
        Employe employe = mapper.toEntity(dto);

        // Gérer le service
        if (dto.getServiceId() != null) {
            com.codeWithProject.ecom.entity.Service service = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", dto.getServiceId()));
            employe.setService(service);
        }

        // Gérer le manager
        if (dto.getManagerId() != null) {
            Manager manager = managerRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", dto.getManagerId()));
            employe.setManager(manager);
        }

        // CRITICAL: Créer et associer l'utilisateur
        String defaultPassword = dto.getPassword() != null ? dto.getPassword() : "default123";
        String encodedPassword = passwordEncoder.encode(defaultPassword);
        employe.createUtilisateur(encodedPassword);

        // Sauvegarder (cascade sauvegardera aussi l'utilisateur)
        Employe saved = employeRepository.save(employe);

        log.info("Employé créé avec succès - ID: {}, Utilisateur associé ID: {}",
                saved.getId(), saved.getUtilisateur() != null ? saved.getUtilisateur().getId() : null);

        return mapper.toDto(saved);
    }

    @Override
    public EmployeDTO update(Long id, EmployeDTO dto) {
        log.debug("Mise à jour de l'employé ID: {}", id);

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        // Mise à jour des champs
        if (dto.getMatricule() != null && !dto.getMatricule().equals(employe.getMatricule())) {
            if (employeRepository.existsByMatricule(dto.getMatricule())) {
                throw new BusinessException("Un employé avec ce matricule existe déjà");
            }
            employe.setMatricule(dto.getMatricule());
        }

        if (dto.getNom() != null) employe.setNom(dto.getNom());
        if (dto.getPrenom() != null) employe.setPrenom(dto.getPrenom());
        if (dto.getEmail() != null) {
            if (!dto.getEmail().equals(employe.getEmail()) && utilisateurRepository.existsByEmail(dto.getEmail())) {
                throw new BusinessException("Un utilisateur avec cet email existe déjà");
            }
            employe.setEmail(dto.getEmail());
        }
        if (dto.getTelephone() != null) employe.setTelephone(dto.getTelephone());
        if (dto.getPoste() != null) employe.setPoste(dto.getPoste());
        if (dto.getSalaire() != null) employe.setSalaire(dto.getSalaire());
        if (dto.getDepartement() != null) employe.setDepartement(dto.getDepartement());
        if (dto.getStatut() != null) employe.setStatut(dto.getStatut());
        if (dto.getSoldeConges() != null) employe.setSoldeConges(dto.getSoldeConges());
        if (dto.getDateEmbauche() != null) employe.setDateEmbauche(dto.getDateEmbauche());

        // Synchroniser avec l'utilisateur associé
        if (employe.getUtilisateur() != null) {
            employe.getUtilisateur().setNom(employe.getNom());
            employe.getUtilisateur().setPrenom(employe.getPrenom());
            employe.getUtilisateur().setEmail(employe.getEmail());
            employe.getUtilisateur().setTelephone(employe.getTelephone());
        }

        if (dto.getServiceId() != null) {
            com.codeWithProject.ecom.entity.Service service = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", dto.getServiceId()));
            employe.setService(service);
        }

        if (dto.getManagerId() != null) {
            Manager manager = managerRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", dto.getManagerId()));
            employe.setManager(manager);
        }

        Employe saved = employeRepository.save(employe);
        log.info("Employé mis à jour - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement) {
        log.debug("Mise à jour du profil ID: {}", id);

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        if (poste != null) employe.setPoste(poste);
        if (salaire != null) employe.setSalaire(salaire);
        if (departement != null) employe.setDepartement(departement);

        return mapper.toDto(employeRepository.save(employe));
    }

    @Override
    public EmployeDTO changerStatut(Long id, String nouveauStatut) {
        log.debug("Changement statut ID: {} -> {}", id, nouveauStatut);

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        employe.setStatut(nouveauStatut);
        return mapper.toDto(employeRepository.save(employe));
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression employé ID: {}", id);

        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        // L'utilisateur sera supprimé automatiquement grâce à cascade et orphanRemoval
        employeRepository.delete(employe);
        log.info("Employé supprimé - ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByDepartement() {
        return employeRepository.countByDepartement().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatut() {
        return employeRepository.countByStatut().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
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
        return employeRepository.findEmployesRecents().stream()
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
        return employeRepository.searchEmployes(keyword.trim()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TableauBordEmployeDTO getStatsTableauBord() {
        log.info("Récupération des statistiques tableau de bord des employés");

        List<Object[]> stats = employeRepository.getStatsTableauBord();

        if (stats == null || stats.isEmpty()) {
            log.warn("Aucune donnée de statistiques trouvée");
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

        Object[] stat = stats.get(0);

        // Vérifier la longueur du tableau
        if (stat.length < 7) {
            log.error("Le résultat de la requête a {} colonnes, mais 7 sont attendues", stat.length);
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

        Long total = stat[0] != null ? ((Number) stat[0]).longValue() : 0L;
        Long actifs = stat[1] != null ? ((Number) stat[1]).longValue() : 0L;
        Long inactifs = stat[2] != null ? ((Number) stat[2]).longValue() : 0L;
        Long enConge = stat[3] != null ? ((Number) stat[3]).longValue() : 0L;
        Double salaireMoyen = stat[4] != null ? ((Number) stat[4]).doubleValue() : 0.0;
        Double masseSalariale = stat[5] != null ? ((Number) stat[5]).doubleValue() : 0.0;
        Double soldeCongesMoyen = stat[6] != null ? ((Number) stat[6]).doubleValue() : 0.0;

        log.info("Statistiques récupérées: total={}, actifs={}, salaireMoyen={}", total, actifs, salaireMoyen);

        return TableauBordEmployeDTO.builder()
                .total(total)
                .actifs(actifs)
                .inactifs(inactifs)
                .enConge(enConge)
                .salaireMoyen(salaireMoyen)
                .masseSalariale(masseSalariale)
                .soldeCongesMoyen(soldeCongesMoyen)
                .build();
    }

    // ===== NOUVELLES MÉTHODES =====

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeDTO> findByEmail(String email) {
        log.debug("Recherche d'employé par email: {}", email);
        return utilisateurRepository.findByEmail(email)
                .map(Utilisateur::getEmploye)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SoldeCongesDTO getSoldeCongesByEmail(String email) {
        log.debug("Récupération du solde de congés pour: {}", email);

        Employe employe = utilisateurRepository.findByEmail(email)
                .map(Utilisateur::getEmploye)
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
    public List<CompetenceEmployeDTO> getCompetencesByEmail(String email) {
        log.debug("Récupération des compétences pour: {}", email);

        Employe employe = utilisateurRepository.findByEmail(email)
                .map(Utilisateur::getEmploye)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return employeCompetenceRepository.findByEmployeId(employe.getId()).stream()
                .map(ec -> CompetenceEmployeDTO.builder()
                        .id(ec.getCompetence().getId())
                        .nom(ec.getCompetence().getNom())
                        .categorie(ec.getCompetence().getCategorie())
                        .niveau(ec.getNiveau())
                        .certifie(ec.getCertifie())
                        .dateAcquisition(ec.getDateAcquisition())
                        .dateExpiration(ec.getDateExpirationCertification())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationEmployeDTO> getFormationsByEmail(String email) {
        log.debug("Récupération des formations pour: {}", email);

        Employe employe = utilisateurRepository.findByEmail(email)
                .map(Utilisateur::getEmploye)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return formationRepository.findFormationsByEmployeId(employe.getId()).stream()
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
        log.debug("Récupération de l'historique des congés pour: {}", email);

        Employe employe = utilisateurRepository.findByEmail(email)
                .map(Utilisateur::getEmploye)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé"));

        return demandeCongeRepository.findByEmployeId(employe.getId()).stream()
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
        log.debug("Mise à jour du profil pour: {}", email);

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
            if (user.getEmploye() != null) {
                user.getEmploye().setTelephone(request.getTelephone());
            }
            utilisateurRepository.save(user);
        }

        return mapper.toDto(user.getEmploye());
    }

    @Override
    public void changePasswordByEmail(String email, ChangePasswordRequest request) {
        log.debug("Changement de mot de passe pour: {}", email);

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        utilisateurRepository.save(user);
    }

    @Override
    public EmployeDTO changeEmailByEmail(String email, String newEmail) {
        log.debug("Changement d'email pour: {} vers {}", email, newEmail);

        if (utilisateurRepository.existsByEmail(newEmail)) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        user.setEmail(newEmail);
        if (user.getEmploye() != null) {
            user.getEmploye().setEmail(newEmail);
        }
        utilisateurRepository.save(user);

        return mapper.toDto(user.getEmploye());
    }
}