package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.AdministrateurRHService;
import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import com.codeWithProject.ecom.service.dto.CompetenceDTO;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.AdministrateurRHMapper;
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
@Transactional
@Slf4j
public class AdministrateurRHServiceImpl implements AdministrateurRHService {

    private final AdministrateurRHRepository administrateurRHRepository;
    private final EmployeRepository employeRepository;
    private final AdministrateurRHMapper mapper;

    // ── Lecture ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdministrateurRHDTO> findAll() {
        log.debug("Récupération de tous les administrateurs");
        return administrateurRHRepository.findAllWithDetails().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdministrateurRHDTO> findAll(Pageable pageable) {
        log.debug("Récupération des administrateurs page={}", pageable.getPageNumber());
        Page<AdministrateurRH> page = administrateurRHRepository.findAll(pageable);
        List<AdministrateurRHDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findById(Long id) {
        return administrateurRHRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findByEmployeId(Long employeId) {
        // Un administrateur est lui-même un employé, donc son ID est l'ID employé
        return administrateurRHRepository.findById(employeId).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findByEmployeMatricule(String matricule) {
        return administrateurRHRepository.findByMatricule(matricule).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrateurRHDTO> findByEmail(String email) {
        return administrateurRHRepository.findByEmail(email).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAdministrateur(String email) {
        return administrateurRHRepository.isEmailAdministrateur(email);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return administrateurRHRepository.countAdministrateurs();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrateurRHDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        return administrateurRHRepository.searchAdministrateurs(keyword.trim()).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ── Écriture ───────────────────────────────────────────────

    @Override
    public AdministrateurRHDTO create(AdministrateurRHDTO dto) {
        log.debug("Création d'un nouvel administrateur");

        if (dto.getEmployeId() == null) {
            throw new BusinessException("L'ID de l'employé est obligatoire");
        }

        // Récupérer l'employé existant
        Employe employe = employeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

        // Vérifier s'il est déjà administrateur
        if (administrateurRHRepository.existsById(employe.getId())) {
            throw new BusinessException("Cet employé est déjà administrateur");
        }

        // Créer un administrateur en copiant toutes les propriétés de l'employé
        AdministrateurRH admin = new AdministrateurRH();
        admin.setId(employe.getId()); // Important : même ID pour la stratégie JOINED
        admin.setMatricule(employe.getMatricule());
        admin.setNom(employe.getNom());
        admin.setPrenom(employe.getPrenom());
        admin.setEmail(employe.getEmail());
        admin.setTelephone(employe.getTelephone());
        admin.setPassword(employe.getPassword());
        admin.setDateEmbauche(employe.getDateEmbauche());
        admin.setPoste(employe.getPoste());
        admin.setSalaire(employe.getSalaire());
        admin.setStatut(employe.getStatut());
        admin.setDepartement(employe.getDepartement());
        admin.setSoldeConges(employe.getSoldeConges());
        admin.setActif(employe.getActif());
        admin.setDateCreation(employe.getDateCreation());
        admin.setDerniereConnexion(employe.getDerniereConnexion());
        admin.setNombreConnexions(employe.getNombreConnexions());
        admin.setTentativesEchec(employe.getTentativesEchec());
        admin.setCompteVerrouille(employe.getCompteVerrouille());
        admin.setDateVerrouillage(employe.getDateVerrouillage());
        admin.setRole("ADMIN_RH");
        admin.setManager(employe.getManager());
        admin.setService(employe.getService());
        // Les collections (competences, formations, demandesConge) sont conservées

        // Sauvegarder l'administrateur (cela va insérer une ligne dans administrateurs_rh)
        AdministrateurRH saved = administrateurRHRepository.save(admin);

        // Supprimer l'ancien employé (optionnel : on pourrait le garder, mais l'ID est le même)
        // employeRepository.delete(employe); // Attention : cascade ? Plutôt ne pas supprimer.

        log.info("Administrateur créé à partir de l'employé id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    public AdministrateurRHDTO update(Long id, AdministrateurRHDTO dto) {
        log.debug("Mise à jour de l'administrateur id={}", id);

        AdministrateurRH admin = administrateurRHRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur", id));

        if (dto.getNom() != null) admin.setNom(dto.getNom());
        if (dto.getPrenom() != null) admin.setPrenom(dto.getPrenom());
        if (dto.getEmail() != null) admin.setEmail(dto.getEmail());
        if (dto.getTelephone() != null) admin.setTelephone(dto.getTelephone());
        if (dto.getActif() != null) admin.setActif(dto.getActif());

        // Mise à jour d'autres champs si nécessaire
        if (dto.getPoste() != null) admin.setPoste(dto.getPoste());
        if (dto.getSalaire() != null) {
    admin.setSalaire(java.math.BigDecimal.valueOf(dto.getSalaire()));
}
        if (dto.getDepartement() != null) admin.setDepartement(dto.getDepartement());

        AdministrateurRH saved = administrateurRHRepository.save(admin);
        log.info("Administrateur mis à jour id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    public AdministrateurRHDTO assignerEmploye(Long adminId, Long employeId) {
        // Cette méthode n'a plus de sens car un administrateur est un employé.
        // On peut simplement vérifier que l'employé existe et le promouvoir.
        log.warn("assignerEmploye est obsolète - promotion automatique via create");
        throw new UnsupportedOperationException("Utilisez create() pour promouvoir un employé en administrateur");
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression administrateur id={}", id);
        if (!administrateurRHRepository.existsById(id)) {
            throw new ResourceNotFoundException("Administrateur", id);
        }
        administrateurRHRepository.deleteById(id);
        log.info("Administrateur supprimé id={}", id);
    }

    // ── Métier (délégation) ────────────────────────────────────
    // (les méthodes suivantes sont inchangées)
    @Override
    public void creerEmploye(EmployeDTO employeDto) { log.debug("creerEmploye — déléguer à EmployeService"); }
    @Override
    public void modifierEmploye(Long employeId, EmployeDTO employeDto) { log.debug("modifierEmploye id={}", employeId); }
    @Override
    public void supprimerEmploye(Long employeId) { log.debug("supprimerEmploye id={}", employeId); }
    @Override
    public void consulterEmploye(Long employeId) { log.debug("consulterEmploye id={}", employeId); }
    @Override
    public void creerCompetence(CompetenceDTO competenceDto) { log.debug("creerCompetence — déléguer à CompetenceService"); }
    @Override
    public void modifierCompetence(Long competenceId, CompetenceDTO competenceDto) { log.debug("modifierCompetence id={}", competenceId); }
    @Override
    public void supprimerCompetence(Long competenceId) { log.debug("supprimerCompetence id={}", competenceId); }
    @Override
    public void associerCompetenceEmploye(Long employeId, Long competenceId, String niveau) { log.debug("associerCompetenceEmploye"); }
    @Override
    public void creerFormation(FormationDTO formationDto) { log.debug("creerFormation — déléguer à FormationService"); }
    @Override
    public void modifierFormation(Long formationId, FormationDTO formationDto) { log.debug("modifierFormation id={}", formationId); }
    @Override
    public void supprimerFormation(Long formationId) { log.debug("supprimerFormation id={}", formationId); }
    @Override
    public void consulterDashboardsRH() { log.debug("consulterDashboardsRH"); }
    @Override
    public void analyserTurnover() { log.debug("analyserTurnover — déléguer à SystemeBIService"); }
    @Override
    public void analyserAbsenteisme() { log.debug("analyserAbsenteisme — déléguer à SystemeBIService"); }
    @Override
    public void consulterScoresRisque() { log.debug("consulterScoresRisque — déléguer à SystemeBIService"); }
}