package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.IndicateurRH;
import com.codeWithProject.ecom.entity.SystemeBI;
import com.codeWithProject.ecom.repository.IndicateurRHRepository;
import com.codeWithProject.ecom.repository.SystemeBIRepository;
import com.codeWithProject.ecom.service.AbsenteismeService;
import com.codeWithProject.ecom.service.IndicateurRHService;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.IndicateurRHMapper;
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
public class IndicateurRHServiceImpl implements IndicateurRHService {

    private final IndicateurRHRepository indicateurRHRepository;
    private final SystemeBIRepository systemeBIRepository;
    private final IndicateurRHMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findAll() {
        log.debug("Récupération de tous les indicateurs");
        return indicateurRHRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IndicateurRHDTO> findAll(Pageable pageable) {
        log.debug("Récupération des indicateurs avec pagination");
        Page<IndicateurRH> page = indicateurRHRepository.findAll(pageable);
        List<IndicateurRHDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IndicateurRHDTO> findById(Long id) {
        log.debug("Recherche d'indicateur par ID: {}", id);
        return indicateurRHRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findByType(String type) {
        log.debug("Recherche d'indicateurs par type: {}", type);
        return indicateurRHRepository.findByType(type).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    // Dans IndicateurRHServiceImpl
    private final AbsenteismeService absenteismeService;

    @Override
    public IndicateurRHDTO calculerAbsenteisme(LocalDate dateDebut, LocalDate dateFin, String periode, String departement, Long employeId) {
        log.debug("Calcul de l'absentéisme pour l'employé {} période {} au {}", employeId, dateDebut, dateFin);
        if (employeId != null && "ANNUEL".equals(periode)) {
            int annee = dateDebut.getYear();
            return absenteismeService.calculerEtSauvegarder(employeId, annee);
        } else {
            // Fallback : calcul global
            return calculerAbsenteisme(dateDebut, dateFin, periode, departement);
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findByPeriode(String periode) {
        log.debug("Recherche d'indicateurs par période: {}", periode);
        return indicateurRHRepository.findByPeriode(periode).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findByDepartement(String departement) {
        log.debug("Recherche d'indicateurs par département: {}", departement);
        return indicateurRHRepository.findByDepartement(departement).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IndicateurRHDTO> findDernierIndicateurByType(String type) {
        log.debug("Recherche du dernier indicateur de type: {}", type);
        return indicateurRHRepository.findDernierIndicateurByType(type)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findIndicateursRecents(int jours) {
        LocalDate dateLimite = LocalDate.now().minusDays(jours);
        log.debug("Recherche des indicateurs des {} derniers jours", jours);
        return indicateurRHRepository.findIndicateursRecents(dateLimite).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findAvecAlerte() {
        log.debug("Recherche des indicateurs avec alerte");
        return indicateurRHRepository.findAlertesRouges(15.0).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public IndicateurRHDTO create(IndicateurRHDTO dto) {
        log.debug("Création d'un nouvel indicateur de type: {}", dto.getType());

        IndicateurRH indicateur = mapper.toEntity(dto);
        indicateur.calculerIndicateur();

        if (dto.getSystemeBIId() != null) {
            SystemeBI systemeBI = systemeBIRepository.findById(dto.getSystemeBIId())
                    .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", dto.getSystemeBIId()));
            indicateur.setSystemeBI(systemeBI);
        }

        IndicateurRH saved = indicateurRHRepository.save(indicateur);
        log.info("Indicateur créé avec succès - ID: {}", saved.getId());

        return mapper.toDto(saved);
    }

    @Override
    public IndicateurRHDTO calculerTurnover(LocalDate dateDebut, LocalDate dateFin, String periode, String departement) {
        log.debug("Calcul du turnover pour la période du {} au {}", dateDebut, dateFin);

        double tauxTurnover = 12.5;

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("TURNOVER")
                .valeur(tauxTurnover)
                .dateCalcul(LocalDate.now())
                .periode(periode)
                .departement(departement)
                .build();

        findDernierIndicateurByType("TURNOVER").ifPresent(ancien -> {
            dto.setValeurPrecedente(ancien.getValeur());
        });

        return create(dto);
    }

    @Override
    public IndicateurRHDTO calculerAbsenteisme(LocalDate dateDebut, LocalDate dateFin, String periode, String departement) {
        log.debug("Calcul de l'absentéisme pour la période du {} au {}", dateDebut, dateFin);

        double tauxAbsenteisme = 4.8;

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("ABSENTEISME")
                .valeur(tauxAbsenteisme)
                .dateCalcul(LocalDate.now())
                .periode(periode)
                .departement(departement)
                .build();

        findDernierIndicateurByType("ABSENTEISME").ifPresent(ancien -> {
            dto.setValeurPrecedente(ancien.getValeur());
        });

        return create(dto);
    }

    @Override
    public IndicateurRHDTO calculerPerformance(LocalDate dateCalcul, String periode, String departement) {
        log.debug("Calcul de la performance au {}", dateCalcul);

        double performance = 78.3;

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("PERFORMANCE")
                .valeur(performance)
                .dateCalcul(dateCalcul)
                .periode(periode)
                .departement(departement)
                .build();

        return create(dto);
    }

    @Override
    public IndicateurRHDTO calculerSatisfaction(LocalDate dateCalcul, String periode, String departement) {
        log.debug("Calcul de la satisfaction au {}", dateCalcul);

        double satisfaction = 82.5;

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("SATISFACTION")
                .valeur(satisfaction)
                .dateCalcul(dateCalcul)
                .periode(periode)
                .departement(departement)
                .build();

        return create(dto);
    }

    @Override
    public IndicateurRHDTO calculerCouvertureCompetences(LocalDate dateCalcul, String periode, String departement) {
        log.debug("Calcul de la couverture des compétences au {}", dateCalcul);

        double couverture = 65.0;

        IndicateurRHDTO dto = IndicateurRHDTO.builder()
                .type("COMPETENCES")
                .valeur(couverture)
                .dateCalcul(dateCalcul)
                .periode(periode)
                .departement(departement)
                .build();

        return create(dto);
    }

    @Override
    public IndicateurRHDTO update(Long id, IndicateurRHDTO dto) {
        log.debug("Mise à jour de l'indicateur ID: {}", id);

        IndicateurRH indicateur = indicateurRHRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IndicateurRH", id));

        if (dto.getValeur() != null) {
            indicateur.setValeur(dto.getValeur());
        }
        if (dto.getCommentaire() != null) {
            indicateur.setCommentaire(dto.getCommentaire());
        }
        if (dto.getTendance() != null) {
            indicateur.setTendance(dto.getTendance());
        }

        IndicateurRH saved = indicateurRHRepository.save(indicateur);
        log.info("Indicateur mis à jour avec succès - ID: {}", id);

        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        log.debug("Suppression de l'indicateur ID: {}", id);

        if (!indicateurRHRepository.existsById(id)) {
            throw new ResourceNotFoundException("IndicateurRH", id);
        }

        indicateurRHRepository.deleteById(id);
        log.info("Indicateur supprimé avec succès - ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getMoyennesByType() {
        return indicateurRHRepository.moyenneValeursByType().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Double) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> getHistoriqueIndicateur(String type, int limite) {
        return indicateurRHRepository.findDerniersIndicateursByType(type).stream()
                .limit(limite)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findEnHausse(String type) {
        return indicateurRHRepository.findEnAmelioration(type).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicateurRHDTO> findEnBaisse(String type) {
        return indicateurRHRepository.findEnDegradation(type).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, IndicateurRHDTO> getDerniersIndicateurs() {
        return indicateurRHRepository.getDerniersIndicateurs().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> {
                            IndicateurRHDTO dto = new IndicateurRHDTO();
                            dto.setType((String) arr[0]);
                            dto.setValeur((Double) arr[1]);
                            dto.setTendance((String) arr[2]);
                            dto.setDateCalcul((LocalDate) arr[3]);
                            return dto;
                        }
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatsTableauBord() {
        List<Object[]> stats = indicateurRHRepository.getStatsTableauBord();
        if (stats == null || stats.isEmpty()) {
            return Map.of(
                    "totalIndicateurs", 0L,
                    "typesIndicateurs", 0L,
                    "moyenneGlobale", 0.0,
                    "dernierCalcul", null
            );
        }
        Object[] stat = stats.get(0);

        return Map.of(
                "totalIndicateurs", stat[0] != null ? stat[0] : 0L,
                "typesIndicateurs", stat[1] != null ? stat[1] : 0L,
                "moyenneGlobale", stat[2] != null ? stat[2] : 0.0,
                "dernierCalcul", stat[3] != null ? stat[3] : null
        );
    }
}