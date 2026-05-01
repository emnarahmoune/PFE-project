package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Competence;
import com.codeWithProject.ecom.repository.CompetenceRepository;
import com.codeWithProject.ecom.service.CompetenceService;
import com.codeWithProject.ecom.service.dto.CompetenceDTO;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import com.codeWithProject.ecom.service.mapper.CompetenceMapper;
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
public class CompetenceServiceImpl implements CompetenceService {

    private final CompetenceRepository competenceRepository;
    private final CompetenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CompetenceDTO> findAll() {
        log.debug("Récupération de toutes les compétences");
        return competenceRepository.findAllOrderByNomAsc().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompetenceDTO> findAll(Pageable pageable) {
        log.debug("Récupération des compétences avec pagination");
        Page<Competence> page = competenceRepository.findAll(pageable);
        List<CompetenceDTO> dtos = page.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<CompetenceDTO> findByNom(String nom) {
        log.debug("Recherche de compétence par nom : {}", nom);
        return competenceRepository.findByNom(nom)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetenceDTO> findByCategorie(String categorie) {
        log.debug("Recherche de compétences par catégorie : {}", categorie);
        return competenceRepository.findByCategorie(categorie).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllCategories() {
        log.debug("Récupération de toutes les catégories");
        return competenceRepository.findAllCategories();
    }

    @Override
    public CompetenceDTO create(CompetenceDTO dto) {
        log.debug("Création d'une nouvelle compétence: {}", dto.getNom());

        // Validation
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new BusinessException("Le nom de la compétence est obligatoire");
        }

        // Vérifier l'unicité du nom
        if (competenceRepository.existsByNom(dto.getNom())) {
            throw new BusinessException("Une compétence avec ce nom existe déjà");
        }

        Competence competence = mapper.toEntity(dto);
        Competence saved = competenceRepository.save(competence);
        log.info("Compétence créée avec succès - ID: {}", saved.getId());

        return mapper.toDto(saved);
    }

    @Override
    public CompetenceDTO update(Long id, CompetenceDTO dto) {
        log.debug("Mise à jour de la compétence ID: {}", id);

        Competence competence = competenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compétence", id));

        // Vérifier l'unicité du nom si modifié
        if (dto.getNom() != null && !dto.getNom().equals(competence.getNom())) {
            if (competenceRepository.existsByNom(dto.getNom())) {
                throw new BusinessException("Une compétence avec ce nom existe déjà");
            }
            competence.setNom(dto.getNom());
        }

        // Mise à jour des champs
        if (dto.getDescription() != null) {
            competence.setDescription(dto.getDescription());
        }
        if (dto.getCategorie() != null) {
            competence.setCategorie(dto.getCategorie());
        }

        Competence saved = competenceRepository.save(competence);
        log.info("Compétence mise à jour avec succès - ID: {}", id);

        return mapper.toDto(saved);
    }

   

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNom(String nom) {
        return competenceRepository.existsByNom(nom);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return competenceRepository.countTotalCompetences();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatsByCategorie() {
        log.debug("Calcul des statistiques par catégorie");
        return competenceRepository.countByCategorie().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetenceDTO> search(String keyword) {
        log.debug("Recherche de compétences avec mot-clé: {}", keyword);
        return competenceRepository.searchCompetences(keyword).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetenceDTO> findTopCompetences(int limit) {
        log.debug("Récupération des {} compétences les plus utilisées", limit);
        return competenceRepository.findCompetencesLesPlusUtilisees().stream()
                .limit(limit)
                .map(obj -> {
                    Competence c = (Competence) obj[0];
                    return mapper.toDto(c);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetenceDTO> findCompetencesNonAttribuees() {
        log.debug("Récupération des compétences non attribuées");
        return competenceRepository.findCompetencesNonAttribuees().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }




    @Override
public Optional<CompetenceDTO> findById(Long id) {

    Competence c = competenceRepository.findByIdWithEmployes(id)
        .orElseThrow(() -> new RuntimeException("Compétence introuvable"));

    CompetenceDTO dto = new CompetenceDTO();

    dto.setId(c.getId());
    dto.setNom(c.getNom());
    dto.setDescription(c.getDescription());
    dto.setCategorie(c.getCategorie());

    // 🔥 NOMBRE
   dto.setNombreEmployes((long) c.getEmployes().size());

    // 🔥 LISTE EMPLOYES
    List<EmployeDTO> employes = c.getEmployes().stream().map(e -> {
        EmployeDTO emp = new EmployeDTO();
        emp.setId(e.getId());
        emp.setNom(e.getNom());
        emp.setPrenom(e.getPrenom());
        emp.setPoste(e.getPoste());
        return emp;
    }).toList();

    dto.setEmployes(employes);

    return Optional.of(dto);
}
public void delete(Long id) {
    Competence competence = competenceRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Compétence introuvable"));

    // 🔥 IMPORTANT : vider les relations
    competence.getEmployes().clear();

    competenceRepository.save(competence);

    competenceRepository.delete(competence);
}
}