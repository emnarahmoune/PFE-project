package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.ManagerService;
import com.codeWithProject.ecom.service.dto.ManagerDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ManagerMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findAll() {
        log.debug("Récupération de tous les managers");
        return managerRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagerDTO> findAll(Pageable pageable) {
        log.debug("Récupération des managers avec pagination");
        Page<Manager> page = managerRepository.findAll(pageable);
        List<ManagerDTO> dtos = page.getContent().stream()
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
        return managerRepository.findByEmployeId(employeId)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagerDTO> findByEmployeMatricule(String matricule) {
        log.debug("Recherche de manager par matricule: {}", matricule);
        return managerRepository.findByEmployeMatricule(matricule)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findByDepartement(String departement) {
        log.debug("Recherche de managers par département: {}", departement);
        return managerRepository.findByDepartement(departement).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersActifs() {
        log.debug("Recherche des managers actifs");
        return managerRepository.findManagersActifs().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersSansEquipe() {
        log.debug("Recherche des managers sans équipe");
        return managerRepository.findManagersSansEquipe().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersAvecDemandesEnAttente() {
        log.debug("Recherche des managers avec demandes en attente");
        return managerRepository.findManagersAvecDemandesEnAttente().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersAvecDemandesUrgentes() {
        log.debug("Recherche des managers avec demandes urgentes");
        return managerRepository.findManagersAvecDemandesUrgentes().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ManagerDTO create(ManagerDTO dto) {
        log.debug("Création d'un nouveau manager");

        // Validation
        if (dto.getDepartement() == null || dto.getDepartement().trim().isEmpty()) {
            throw new BusinessException("Le département est obligatoire");
        }

        Manager manager = mapper.toEntity(dto);

        // Association à un employé si spécifié
        if (dto.getEmployeId() != null) {
            Employe employe = employeRepository.findById(dto.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));
            manager.setEmploye(employe);
        }

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
        Manager saved = managerRepository.save(manager);

        return mapper.toDto(saved);
    }

    @Override
    public ManagerDTO desactiver(Long id) {
        log.debug("Désactivation du manager ID: {}", id);

        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", id));

        manager.desactiver();
        Manager saved = managerRepository.save(manager);

        return mapper.toDto(saved);
    }

    @Override
    public ManagerDTO ajouterEmploye(Long managerId, Long employeId) {
        log.debug("Ajout de l'employé {} à l'équipe du manager {}", employeId, managerId);

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

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
        List<Object[]> stats = managerRepository.getManagersStats();
        if (stats.isEmpty()) {
            return Map.of();
        }
        Object[] stat = stats.get(0);

        return Map.of(
                "totalManagers", stat[0],
                "managersActifs", stat[1],
                "tailleMoyenneEquipe", stat[2],
                "totalEmployesGeres", stat[3],
                "departementsCouverts", stat[4]
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByDepartement() {
        return managerRepository.countManagersByDepartement().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
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
        return managerRepository.searchManagers(keyword).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerDTO> findManagersRecents(int limit) {
        return managerRepository.findManagersRecents().stream()
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
}