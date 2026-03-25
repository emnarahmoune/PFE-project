package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.IndicateurRH;
import com.codeWithProject.ecom.entity.ScoreTurnover;
import com.codeWithProject.ecom.entity.SystemeBI;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.FormationRepository;
import com.codeWithProject.ecom.repository.SystemeBIRepository;
import com.codeWithProject.ecom.service.SystemeBIService;
import com.codeWithProject.ecom.service.dto.FormationDTO;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import com.codeWithProject.ecom.service.dto.ScoreTurnoverDTO;
import com.codeWithProject.ecom.service.dto.SystemeBIDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.FormationMapper;
import com.codeWithProject.ecom.service.mapper.IndicateurRHMapper;
import com.codeWithProject.ecom.service.mapper.ScoreTurnoverMapper;
import com.codeWithProject.ecom.service.mapper.SystemeBIMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SystemeBIServiceImpl implements SystemeBIService {

    private final SystemeBIRepository systemeBIRepository;
    private final EmployeRepository employeRepository;
    private final FormationRepository formationRepository;
    private final SystemeBIMapper mapper;
    private final FormationMapper formationMapper;
    private final IndicateurRHMapper indicateurMapper;
    private final ScoreTurnoverMapper scoreMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SystemeBIDTO> findAll() {
        log.debug("Récupération de tous les systèmes BI");
        return systemeBIRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SystemeBIDTO> findAll(Pageable pageable) {
        log.debug("Récupération des systèmes BI avec pagination");
        Page<SystemeBI> page = systemeBIRepository.findAll(pageable);
        List<SystemeBIDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemeBIDTO> findById(Long id) {
        log.debug("Recherche de système BI par ID: {}", id);
        return systemeBIRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemeBIDTO> findByVersion(String version) {
        log.debug("Recherche de système BI par version: {}", version);
        return systemeBIRepository.findByVersion(version)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemeBIDTO> findSystemesActifs() {
        log.debug("Recherche des systèmes BI actifs");
        return systemeBIRepository.findSystemesActifs().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemeBIDTO> findSystemesEnMaintenance() {
        log.debug("Recherche des systèmes BI en maintenance");
        return systemeBIRepository.findSystemesEnMaintenance().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SystemeBIDTO create(SystemeBIDTO dto) {
        log.debug("Création d'un nouveau système BI version: {}", dto.getVersion());

        // Validation
        if (dto.getVersion() == null || dto.getVersion().trim().isEmpty()) {
            throw new BusinessException("La version est obligatoire");
        }

        if (systemeBIRepository.existsByVersion(dto.getVersion())) {
            throw new BusinessException("VERSION_EXISTANTE",
                    "Un système BI avec cette version existe déjà");
        }

        SystemeBI systemeBI = mapper.toEntity(dto);
        SystemeBI saved = systemeBIRepository.save(systemeBI);
        log.info("Système BI créé avec succès - ID: {}, Version: {}", saved.getId(), saved.getVersion());

        return mapper.toDto(saved);
    }

    @Override
    public SystemeBIDTO update(Long id, SystemeBIDTO dto) {
        log.debug("Mise à jour du système BI ID: {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        if (dto.getVersion() != null && !dto.getVersion().equals(systemeBI.getVersion())) {
            if (systemeBIRepository.existsByVersion(dto.getVersion())) {
                throw new BusinessException("VERSION_EXISTANTE",
                        "Un système BI avec cette version existe déjà");
            }
            systemeBI.setVersion(dto.getVersion());
        }

        if (dto.getOutilETL() != null) {
            systemeBI.setOutilETL(dto.getOutilETL());
        }
        if (dto.getOutilVisualisation() != null) {
            systemeBI.setOutilVisualisation(dto.getOutilVisualisation());
        }
        if (dto.getModeleML() != null) {
            systemeBI.setModeleML(dto.getModeleML());
        }
        if (dto.getStatut() != null) {
            systemeBI.setStatut(dto.getStatut());
        }

        SystemeBI saved = systemeBIRepository.save(systemeBI);
        log.info("Système BI mis à jour avec succès - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression du système BI ID: {}", id);

        if (!systemeBIRepository.existsById(id)) {
            throw new ResourceNotFoundException("SystemeBI", id);
        }

        systemeBIRepository.deleteById(id);
        log.info("Système BI supprimé avec succès - ID: {}", id);
    }

    @Override
    public SystemeBIDTO activer(Long id) {
        log.debug("Activation du système BI ID: {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        systemeBI.activer();
        SystemeBI saved = systemeBIRepository.save(systemeBI);

        return mapper.toDto(saved);
    }

    @Override
    public SystemeBIDTO mettreEnMaintenance(Long id) {
        log.debug("Mise en maintenance du système BI ID: {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        systemeBI.mettreEnMaintenance();
        SystemeBI saved = systemeBIRepository.save(systemeBI);

        return mapper.toDto(saved);
    }

    @Override
    public SystemeBIDTO desactiver(Long id) {
        log.debug("Désactivation du système BI ID: {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        systemeBI.desactiver();
        SystemeBI saved = systemeBIRepository.save(systemeBI);

        return mapper.toDto(saved);
    }

    @Override
    public SystemeBIDTO executerETL(Long id) {
        log.debug("Exécution de l'ETL pour le système BI ID: {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        systemeBI.executerETL();
        SystemeBI saved = systemeBIRepository.save(systemeBI);
        log.info("ETL exécuté avec succès - Dernière exécution: {}", saved.getDerniereExecution());

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> analyserCompetences(Long id) {
        log.debug("Analyse des compétences pour le système BI ID: {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        systemeBI.analyserCompetences();

        // Simuler des résultats d'analyse
        return Map.of(
                "totalCompetences", 150,
                "competencesLesPlusCourantes", List.of("Java", "Spring", "Angular"),
                "competencesManquantes", List.of("Python", "React"),
                "tauxCouverture", 0.75
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormationDTO> recommanderFormations(Long id, Long employeId) {
        log.debug("Recommandation de formations pour l'employé {} avec le système BI {}", employeId, id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        List<com.codeWithProject.ecom.entity.Formation> formations =
                formationRepository.findFormationsNonSuiviesParEmploye(employeId);

        return formations.stream()
                .map(formationMapper::toDto)
                .limit(5) // Top 5 recommandations
                .collect(Collectors.toList());
    }

    @Override
    public IndicateurRHDTO analyserTurnover(Long id, String periode, String departement) {
        log.debug("Analyse du turnover pour le système BI {} - Période: {}", id, periode);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        IndicateurRH indicateur = systemeBI.analyserTurnover();
        indicateur.setPeriode(periode);
        indicateur.setDepartement(departement);

        return indicateurMapper.toDto(indicateur);
    }

    @Override
    public IndicateurRHDTO analyserAbsenteisme(Long id, String periode, String departement) {
        log.debug("Analyse de l'absentéisme pour le système BI {} - Période: {}", id, periode);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        IndicateurRH indicateur = systemeBI.analyserAbsenteisme();
        indicateur.setPeriode(periode);
        indicateur.setDepartement(departement);

        return indicateurMapper.toDto(indicateur);
    }

    @Override
    public List<ScoreTurnoverDTO> predireTurnover(Long id) {
        log.debug("Prédiction du turnover pour tous les employés avec le système BI {}", id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        systemeBI.predireTurnover();

        return systemeBI.getScoresTurnover().stream()
                .map(scoreMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ScoreTurnoverDTO predireTurnoverEmploye(Long id, Long employeId) {
        log.debug("Prédiction du turnover pour l'employé {} avec le système BI {}", employeId, id);

        SystemeBI systemeBI = systemeBIRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", id));

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));

        ScoreTurnover score = systemeBI.predireTurnoverEmploye(employe);
        systemeBIRepository.save(systemeBI);

        return scoreMapper.toDto(score);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatut() {
        return systemeBIRepository.countByStatut().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getDerniereExecutionGlobale() {
        return systemeBIRepository.findDerniereExecutionGlobale();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemeBIDTO> search(String keyword) {
        log.debug("Recherche de systèmes BI avec mot-clé: {}", keyword);
        return systemeBIRepository.searchSystemesBI(keyword).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatsTableauBord() {
        List<Object[]> stats = systemeBIRepository.getStatsTableauBord();
        if (stats.isEmpty()) {
            return Map.of();
        }
        Object[] stat = stats.get(0);

        return Map.of(
                "totalSystemes", stat[0],
                "systemesActifs", stat[1],
                "systemesMaintenance", stat[2],
                "systemesInactifs", stat[3],
                "totalIndicateurs", stat[4],
                "totalScores", stat[5],
                "derniereExecution", stat[6]
        );
    }
}