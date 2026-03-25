package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.DemandeCongeService;
import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.DemandeCongeMapper;
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
public class DemandeCongeServiceImpl implements DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final DemandeCongeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findAll() {
        log.debug("Récupération de toutes les demandes de congé");
        return demandeCongeRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeCongeDTO> findAll(Pageable pageable) {
        log.debug("Récupération des demandes avec pagination");
        Page<DemandeConge> page = demandeCongeRepository.findAll(pageable);
        List<DemandeCongeDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DemandeCongeDTO> findById(Long id) {
        log.debug("Recherche de demande par ID: {}", id);
        return demandeCongeRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByEmployeId(Long employeId) {
        log.debug("Recherche des demandes de l'employé ID: {}", employeId);
        return demandeCongeRepository.findByEmployeId(employeId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByManagerId(Long managerId) {
        log.debug("Recherche des demandes du manager ID: {}", managerId);
        return demandeCongeRepository.findByManagerId(managerId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByStatut(String statut) {
        log.debug("Recherche des demandes avec statut: {}", statut);
        return demandeCongeRepository.findByStatut(statut).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findByType(String type) {
        log.debug("Recherche des demandes de type: {}", type);
        return demandeCongeRepository.findByType(type).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findDemandesUrgentes() {
        log.debug("Recherche des demandes urgentes");
        return demandeCongeRepository.findDemandesUrgentes().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findDemandesEnAttentePourManager(Long managerId) {
        log.debug("Recherche des demandes en attente pour le manager ID: {}", managerId);
        return demandeCongeRepository.findDemandesEnAttentePourManager(managerId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeCongeDTO> findCongesEnCours() {
        log.debug("Recherche des congés en cours");
        return demandeCongeRepository.findCongesEnCours().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DemandeCongeDTO create(DemandeCongeDTO dto) {
        log.debug("Création d'une nouvelle demande de congé");

        // Validation des données
        if (dto.getEmployeId() == null) {
            throw new BusinessException("L'ID de l'employé est obligatoire");
        }

        // Récupération de l'employé
        Employe employe = employeRepository.findById(dto.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", dto.getEmployeId()));

        // Vérification des conflits de dates
        if (hasConflitDates(dto.getEmployeId(), dto.getDateDebut(), dto.getDateFin(), null)) {
            throw new BusinessException("CONFLIT_DATES", "Une demande de congé existe déjà sur cette période");
        }

        // Création de la demande
        DemandeConge demande = mapper.toEntity(dto);
        demande.setEmploye(employe);
        demande.soumettre(); // Appel de la méthode métier

        DemandeConge saved = demandeCongeRepository.save(demande);
        log.info("Demande de congé créée avec succès - ID: {}", saved.getId());

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO modifier(Long id, DemandeCongeDTO dto) {
        log.debug("Modification de la demande ID: {}", id);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        // Vérifier que la demande est modifiable (EN_ATTENTE uniquement)
        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("IMPOSSIBLE_MODIFIER",
                    "Seules les demandes en attente peuvent être modifiées");
        }

        // Vérification des conflits de dates (excluant la demande actuelle)
        if (hasConflitDates(demande.getEmploye().getId(),
                dto.getDateDebut() != null ? dto.getDateDebut() : demande.getDateDebut(),
                dto.getDateFin() != null ? dto.getDateFin() : demande.getDateFin(),
                id)) {
            throw new BusinessException("CONFLIT_DATES", "Une demande de congé existe déjà sur cette période");
        }

        // Modification via la méthode métier
        demande.modifier(
                dto.getDateDebut(),
                dto.getDateFin(),
                dto.getType(),
                dto.getCommentaire()
        );

        DemandeConge saved = demandeCongeRepository.save(demande);
        log.info("Demande de congé modifiée avec succès - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO annuler(Long id) {
        log.debug("Annulation de la demande ID: {}", id);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        demande.annuler();
        DemandeConge saved = demandeCongeRepository.save(demande);
        log.info("Demande de congé annulée - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO valider(Long id, Long managerId) {
        log.debug("Validation de la demande ID: {} par le manager ID: {}", id, managerId);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        // Vérifier que le manager est bien celui de l'employé
        if (demande.getEmploye().getManager() == null ||
                !demande.getEmploye().getManager().getId().equals(managerId)) {
            throw new BusinessException("Ce manager n'est pas responsable de cet employé");
        }

        demande.valider();
        demande.setManager(manager);

        // Déduire les jours du solde si congé annuel
        if ("ANNUEL".equals(demande.getType())) {
            demande.getEmploye().deduireConges(demande.getJoursOuvres());
            employeRepository.save(demande.getEmploye());
        }

        DemandeConge saved = demandeCongeRepository.save(demande);
        log.info("Demande de congé validée - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public DemandeCongeDTO refuser(Long id, Long managerId, String motif) {
        log.debug("Refus de la demande ID: {} par le manager ID: {}", id, managerId);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        // Vérifier que le manager est bien celui de l'employé
        if (demande.getEmploye().getManager() == null ||
                !demande.getEmploye().getManager().getId().equals(managerId)) {
            throw new BusinessException("Ce manager n'est pas responsable de cet employé");
        }

        demande.refuserAvecMotif(motif);
        demande.setManager(manager);

        DemandeConge saved = demandeCongeRepository.save(demande);
        log.info("Demande de congé refusée - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression de la demande ID: {}", id);

        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeConge", id));

        // Seul un admin peut supprimer (à vérifier dans le contrôleur)
        demandeCongeRepository.delete(demande);
        log.info("Demande de congé supprimée - ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatut() {
        return demandeCongeRepository.countByStatut().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByType() {
        return demandeCongeRepository.countByType().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getStatsMensuelles(int annee) {
        return demandeCongeRepository.countByMois(annee).stream()
                .collect(Collectors.toMap(
                        arr -> (Integer) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConflitDates(Long employeId, LocalDate debut, LocalDate fin, Long demandeId) {
        List<DemandeConge> demandes = demandeCongeRepository.findByEmployeId(employeId);

        return demandes.stream()
                .filter(d -> demandeId == null || !d.getId().equals(demandeId))
                .filter(d -> "APPROUVE".equals(d.getStatut()) || "EN_ATTENTE".equals(d.getStatut()))
                .anyMatch(d -> !(fin.isBefore(d.getDateDebut()) || debut.isAfter(d.getDateFin())));
    }
}