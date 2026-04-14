package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.FormationRepository;
import com.codeWithProject.ecom.service.FormationService;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.FormationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FormationServiceImpl implements FormationService {

    private final FormationRepository formationRepository;
    private final EmployeRepository employeRepository;
    private final FormationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findAll() {
        return formationRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FormationDTO> findAll(Pageable pageable) {
        Page<Formation> page = formationRepository.findAll(pageable);
        List<FormationDTO> dtos = page.getContent().stream().map(mapper::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormationDTO> findById(Long id) {
        return formationRepository.findById(id).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormationDTO> findByTitre(String titre) {
        return formationRepository.findByTitre(titre).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findByDomaine(String domaine) {
        return formationRepository.findByDomaine(domaine).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findActives() {
        return formationRepository.findByActifTrue().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findFormationsRecentes() {
        return formationRepository.findFormationsRecentes().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public FormationDTO create(FormationDTO dto) {
        if (formationRepository.existsByTitre(dto.getTitre()))
            throw new BusinessException("TITRE_EXISTANT", "Une formation avec ce titre existe déjà");
        if (dto.getTitre() == null || dto.getTitre().trim().isEmpty())
            throw new BusinessException("Le titre de la formation est obligatoire");
        Formation formation = mapper.toEntity(dto);
        Formation saved = formationRepository.save(formation);
        return mapper.toDto(saved);
    }

    @Override
    public FormationDTO update(Long id, FormationDTO dto) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", id));
        if (dto.getTitre() != null && !dto.getTitre().equals(formation.getTitre())) {
            if (formationRepository.existsByTitre(dto.getTitre()))
                throw new BusinessException("TITRE_EXISTANT", "Une formation avec ce titre existe déjà");
            formation.setTitre(dto.getTitre());
        }
        if (dto.getDescription() != null) formation.setDescription(dto.getDescription());
        if (dto.getDomaine() != null) formation.setDomaine(dto.getDomaine());
        if (dto.getDureeHeures() != null) formation.setDureeHeures(dto.getDureeHeures());
        return mapper.toDto(formationRepository.save(formation));
    }

    @Override
    public void delete(Long id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", id));
        formation.setActif(false);
        formationRepository.save(formation);
    }

    @Override
    public FormationDTO activer(Long id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", id));
        formation.setActif(true);
        return mapper.toDto(formationRepository.save(formation));
    }

    @Override
    public FormationDTO desactiver(Long id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", id));
        formation.setActif(false);
        return mapper.toDto(formationRepository.save(formation));
    }

    @Override
    public FormationDTO ajouterParticipant(Long formationId, Long employeId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", formationId));
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));
        if (!formation.getActif())
            throw new BusinessException("FORMATION_INACTIVE", "Impossible d'ajouter un participant à une formation inactive");
        if (formation.getParticipants().contains(employe))
            throw new BusinessException("DEJA_INSCRIT", "Cet employé est déjà inscrit à cette formation");
        formation.getParticipants().add(employe);
        return mapper.toDto(formationRepository.save(formation));
    }

    @Override
    public FormationDTO retirerParticipant(Long formationId, Long employeId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", formationId));
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));
        formation.getParticipants().remove(employe);
        return mapper.toDto(formationRepository.save(formation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findFormationsByEmployeId(Long employeId) {
        return formationRepository.findFormationsByEmployeId(employeId).stream()
                .map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findFormationsNonSuiviesParEmploye(Long employeId) {
        return formationRepository.findFormationsNonSuiviesParEmploye(employeId).stream()
                .map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> findFormationsPopulaires(int limit) {
        return formationRepository.findFormationsLesPlusSuivies().stream()
                .limit(limit)
                .map(obj -> mapper.toDto((Formation) obj[0]))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByDomaine() {
        return formationRepository.countByDomaine().stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> (Long) arr[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculerDureeMoyenne() {
        return formationRepository.dureeMoyenneFormations();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> search(String keyword) {
        return formationRepository.searchFormations(keyword).stream()
                .map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllDomaines() {
        return formationRepository.findAllDomaines();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatsTableauBord() {
        List<Object[]> stats = formationRepository.getStatsTableauBord();
        if (stats == null || stats.isEmpty()) return Map.of();
        Object[] stat = stats.get(0);
        Map<String, Object> result = new HashMap<>();
        if (stat.length > 0) result.put("totalFormations", stat[0]);
        if (stat.length > 1) result.put("formationsActives", stat[1]);
        if (stat.length > 2) result.put("dureeMoyenne", stat[2]);
        if (stat.length > 3) result.put("participantsMoyens", stat[3]);
        if (stat.length > 4) result.put("totalParticipants", stat[4]);
        return result;
    }
}