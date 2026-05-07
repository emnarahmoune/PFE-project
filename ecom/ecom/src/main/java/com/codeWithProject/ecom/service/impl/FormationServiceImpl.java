package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
import com.codeWithProject.ecom.service.FormationService;
import com.codeWithProject.ecom.service.dto.EmployeDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FormationServiceImpl implements FormationService {

    private final FormationRepository formationRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final PosteCompetenceRepository posteCompetenceRepository;
    private final FormationCompetenceRepository formationCompetenceRepository;
    private final EmployeRepository employeRepository;

    // =========================
    // CREATE
    // =========================
    @Override
    public Formation create(Formation formation) {

        if (formation.getActif() == null) {
            formation.setActif(true);
        }

        if (formation.getVideos() != null) {
            int i = 1;

            for (FormationVideo video : formation.getVideos()) {
                video.setFormation(formation);

                if (video.getOrdre() == 0) {
                    video.setOrdre(i++);
                }
            }
        }

        if (formation.getSupports() != null) {
            int i = 1;

            for (FormationSupport support : formation.getSupports()) {
                support.setFormation(formation);

                if (support.getOrdre() == 0) {
                    support.setOrdre(i++);
                }
            }
        }

        return formationRepository.save(formation);
    }

    // =========================
    // GET ALL
    // =========================
    @Override
    public List<Map<String, Object>> getAll() {

        List<Formation> formations = formationRepository.findAllWithDetails();
        long totalEmployes = employeRepository.count();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Formation f : formations) {

            int nbParticipants = employeFormationRepository.countByFormationId(f.getId());

            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("titre", f.getTitre());
            map.put("description", f.getDescription());
            map.put("domaine", f.getDomaine());
            map.put("dureeHeures", f.getDureeHeures());
            map.put("actif", f.getActif());
            map.put("nombreParticipants", nbParticipants);
            map.put("totalEmployes", totalEmployes);

            result.add(map);
        }

        return result;
    }

    // =========================
    // GET BY ID COMPLET
    // =========================
    @Override
    @Transactional
    public Map<String, Object> getByIdComplete(Long id) {

        Formation formation = formationRepository.findByIdWithDetails(id);

        if (formation == null) {
            throw new RuntimeException("Formation introuvable");
        }

        Map<String, Object> response = new HashMap<>();

        response.put("id", formation.getId());
        response.put("titre", formation.getTitre());
        response.put("description", formation.getDescription());
        response.put("domaine", formation.getDomaine());
        response.put("dureeHeures", formation.getDureeHeures());
        response.put("actif", formation.getActif());

        List<Map<String, Object>> videos = new ArrayList<>();

        if (formation.getVideos() != null) {
            for (FormationVideo v : formation.getVideos()) {

                Map<String, Object> item = new HashMap<>();
                item.put("id", v.getId());
                item.put("titre", v.getTitre());
                item.put("urlYoutube", v.getUrlYoutube());
                item.put("ordre", v.getOrdre());

                videos.add(item);
            }
        }

        response.put("videos", videos);

        List<Map<String, Object>> supports = new ArrayList<>();

        if (formation.getSupports() != null) {
            for (FormationSupport s : formation.getSupports()) {

                Map<String, Object> item = new HashMap<>();
                item.put("id", s.getId());
                item.put("titre", s.getTitre());
                item.put("fichierUrl", s.getFichierUrl());
                item.put("ordre", s.getOrdre());

                supports.add(item);
            }
        }

        response.put("supports", supports);

        return response;
    }

    // =========================
    // GET BY ID SIMPLE
    // =========================
    @Override
    public Formation getById(Long id) {
        return formationRepository.findByIdWithDetails(id);
    }

    // =========================
    // UPDATE
    // =========================
    @Override
    public Formation update(Long id, Formation f) {

        Formation formation = formationRepository.findByIdWithDetails(id);

        if (formation == null) {
            throw new RuntimeException("Formation introuvable");
        }

        formation.setTitre(f.getTitre());
        formation.setDescription(f.getDescription());
        formation.setDomaine(f.getDomaine());
        formation.setDureeHeures(f.getDureeHeures());

        if (f.getActif() != null) {
            formation.setActif(f.getActif());
        }

        formation.getVideos().clear();

        if (f.getVideos() != null) {
            for (FormationVideo v : f.getVideos()) {
                v.setId(null);
                v.setFormation(formation);
                formation.getVideos().add(v);
            }
        }

        formation.getSupports().clear();

        if (f.getSupports() != null) {
            for (FormationSupport s : f.getSupports()) {
                s.setId(null);
                s.setFormation(formation);
                formation.getSupports().add(s);
            }
        }

        return formationRepository.save(formation);
    }

    // =========================
    // DELETE
    // =========================
    @Override
    public void delete(Long id) {
        formationRepository.deleteById(id);
    }

    // =========================
    // ACTIVER / DESACTIVER
    // =========================
    @Override
    public void activer(Long id) {
        Formation f = formationRepository.findByIdWithDetails(id);

        if (f == null) {
            throw new RuntimeException("Formation introuvable");
        }

        f.setActif(true);
        formationRepository.save(f);
    }

    @Override
    public void desactiver(Long id) {
        Formation f = formationRepository.findByIdWithDetails(id);

        if (f == null) {
            throw new RuntimeException("Formation introuvable");
        }

        f.setActif(false);
        formationRepository.save(f);
    }

    // =========================
    // FORMATIONS ACTIVES
    // =========================
    @Override
    public List<Formation> getActives() {
        return formationRepository.findByActifTrue();
    }

    // =========================
    // GET PARTICIPANTS
    // =========================
    @Override
    public List<EmployeDTO> getParticipants(Long formationId) {

        return employeFormationRepository.findByFormation_Id(formationId)
                .stream()
                .map(ef -> toEmployeDTO(ef.getEmploye()))
                .filter(Objects::nonNull)
                .toList();
    }

    // =========================
    // GET BY ID AVEC EMPLOYES
    // =========================
    @Override
    public Map<String, Object> getByIdWithEmployes(Long id) {

        Map<String, Object> response = new HashMap<>(getByIdComplete(id));

        List<EmployeDTO> participants = employeFormationRepository
                .findByFormation_Id(id)
                .stream()
                .map(ef -> toEmployeDTO(ef.getEmploye()))
                .filter(Objects::nonNull)
                .toList();

        response.put("employes", participants);
        response.put("nombreParticipants", participants.size());

        return response;
    }

    // =========================
    // FILTER DOMAINE
    // =========================
    @Override
    public List<Formation> getByDomaine(String domaine) {
        return formationRepository.findByDomaine(domaine);
    }

    // =========================
    // FORMATIONS PAR EMPLOYE
    // =========================
    @Override
    public List<Formation> getFormationsByEmploye(Long employeId) {
        return employeFormationRepository.findByEmployeId(employeId)
                .stream()
                .map(EmployeFormation::getFormation)
                .filter(Objects::nonNull)
                .toList();
    }

    // =========================
    // RECO IA = APPEL FLASK /recommend
    // =========================
    @Override
    public List<Formation> getRecommendationsAI(Long employeId) {

        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec id: " + employeId));

        List<EmployeCompetence> competencesEmploye =
                employeCompetenceRepository.findByEmploye_Id(employeId);

        Map<String, Integer> userSkills = new HashMap<>();

        for (EmployeCompetence ec : competencesEmploye) {
            if (ec.getCompetence() != null && ec.getCompetence().getNom() != null) {
                userSkills.put(
                        ec.getCompetence().getNom(),
                        convertLevelToInt(ec.getNiveau())
                );
            }
        }

        Map<String, Integer> requiredSkills = new HashMap<>();

        if (employe.getPoste() != null && !employe.getPoste().isBlank()) {

            String poste = employe.getPoste().trim();

            List<PosteCompetence> competencesPoste =
                    posteCompetenceRepository.findByPosteIgnoreCase(poste);

            if (competencesPoste == null || competencesPoste.isEmpty()) {
                competencesPoste = posteCompetenceRepository.findByPosteContainingIgnoreCase(poste);
            }

            if (competencesPoste != null) {
                for (PosteCompetence pc : competencesPoste) {
                    if (pc.getCompetence() != null && pc.getCompetence().getNom() != null) {
                        requiredSkills.put(
                                pc.getCompetence().getNom(),
                                convertLevelToInt(pc.getNiveauRequis())
                        );
                    }
                }
            }
        }

        List<Formation> formationsNonSuivies =
                formationRepository.findFormationsNonSuivies(employeId);

        if (formationsNonSuivies == null || formationsNonSuivies.isEmpty()) {
            return List.of();
        }

        List<String> formationTitles = formationsNonSuivies.stream()
                .map(Formation::getTitre)
                .filter(Objects::nonNull)
                .toList();

        List<String> formationsSuivies = employeFormationRepository.findByEmployeId(employeId)
                .stream()
                .map(EmployeFormation::getFormation)
                .filter(Objects::nonNull)
                .map(Formation::getTitre)
                .filter(Objects::nonNull)
                .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("userSkills", userSkills);
        payload.put("requiredSkills", requiredSkills);
        payload.put("formations", formationTitles);
        payload.put("formationsSuivies", formationsSuivies);

        try {
            RestTemplate restTemplate = new RestTemplate();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restTemplate.postForObject(
                    "http://localhost:5000/recommend",
                    payload,
                    List.class
            );

           if (response == null || response.isEmpty()) {
    return List.of();
}
            List<String> recommendedTitles = response.stream()
                    .map(item -> item.get("formation"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();

            return formationsNonSuivies.stream()
                    .filter(f -> recommendedTitles.contains(f.getTitre()))
                    .limit(3)
                    .toList();

        } catch (Exception e) {
    e.printStackTrace();
    return List.of();
}
    }

    // =========================
    // RECO PAR COMPETENCES
    // =========================
    @Override
    public List<Formation> getRecommendationsBySkills(Long employeId) {

        List<EmployeCompetence> competencesEmploye =
                employeCompetenceRepository.findByEmploye_Id(employeId);

        if (competencesEmploye == null || competencesEmploye.isEmpty()) {
            return List.of();
        }

        List<Long> competenceIds = competencesEmploye.stream()
                .filter(ec -> ec.getCompetence() != null)
                .map(ec -> ec.getCompetence().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (competenceIds.isEmpty()) {
            return List.of();
        }

        return formationRepository.findRecommendedByCompetenceIds(employeId, competenceIds)
                .stream()
                .limit(3)
                .toList();
    }

    // =========================
    // RECOMMANDATION SIMPLE
    // =========================
    @Override
    public List<Formation> getRecommendations(Long employeId) {
        return getRecommendationsBySkills(employeId);
    }

    // =========================
    // RECOMMANDATION PRINCIPALE
    // =========================
    @Override
    public List<Formation> recommander(Long employeId) {
        return getRecommendationsBySkills(employeId);
    }

    // =========================
    // HELPERS
    // =========================
    private EmployeDTO toEmployeDTO(Employe e) {
        if (e == null) {
            return null;
        }

        String photo = e.getPhotoUrl();

        return EmployeDTO.builder()
        .id(e.getId())
        .matricule(e.getMatricule())
        .nom(e.getNom())
        .prenom(e.getPrenom())
        .email(e.getEmail())
        .telephone(e.getTelephone())
        .poste(e.getPoste())
        .departement(e.getDepartement())
        .statut(e.getStatut())
        .dateEmbauche(e.getDateEmbauche())
        .soldeConges(e.getSoldeConges())

        .photoUrl(photo)
        .employePhotoProfil(photo)

        .build();
    }

    private int convertLevelToInt(String niveau) {
        if (niveau == null || niveau.isBlank()) {
            return 0;
        }

        return switch (niveau.trim().toUpperCase()) {
            case "DEBUTANT" -> 1;
            case "INTERMEDIAIRE" -> 2;
            case "AVANCE" -> 3;
            case "EXPERT" -> 4;
            default -> {
                try {
                    yield Integer.parseInt(niveau.trim());
                } catch (Exception e) {
                    yield 0;
                }
            }
        };
    }

    private int convertLevelToInt(Integer niveau) {
        if (niveau == null) {
            return 0;
        }

        return niveau;
    }





 @Override
public void resetProgress(Long formationId, Authentication auth) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaimAsString("email");

    Employe employe = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    EmployeFormation ef = employeFormationRepository
            .findByEmploye_IdAndFormation_Id(employe.getId(), formationId)
            .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

    ef.setProgression(0);

    employeFormationRepository.save(ef);
}
}