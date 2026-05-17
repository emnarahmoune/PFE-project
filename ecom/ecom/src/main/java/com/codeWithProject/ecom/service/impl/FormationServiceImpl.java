package com.codeWithProject.ecom.service.impl;
import org.springframework.transaction.annotation.Transactional;
import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
import com.codeWithProject.ecom.service.FormationService;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FormationServiceImpl implements FormationService {

    private final FormationRepository formationRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final PosteCompetenceRepository posteCompetenceRepository;
    private final EmployeRepository employeRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.recommendation.url}")
private String aiRecommendationUrl;

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

        for (Formation formation : formations) {
            int nbParticipants = employeFormationRepository.countByFormationId(formation.getId());

            Map<String, Object> map = new HashMap<>();
            map.put("id", formation.getId());
            map.put("titre", formation.getTitre());
            map.put("description", formation.getDescription());
            map.put("domaine", formation.getDomaine());
            map.put("dureeHeures", formation.getDureeHeures());
            map.put("actif", formation.getActif());
            map.put("nombreParticipants", nbParticipants);
            map.put("totalEmployes", totalEmployes);

            result.add(map);
        }

        return result;
    }

    // =========================
    // GET BY ID
    // =========================

    @Override
    public Formation getById(Long id) {
        return formationRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
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
            for (FormationVideo video : formation.getVideos()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", video.getId());
                item.put("titre", video.getTitre());
                item.put("urlYoutube", video.getUrlYoutube());
                item.put("ordre", video.getOrdre());

                videos.add(item);
            }
        }

        response.put("videos", videos);

        List<Map<String, Object>> supports = new ArrayList<>();

        if (formation.getSupports() != null) {
            for (FormationSupport support : formation.getSupports()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", support.getId());
                item.put("titre", support.getTitre());
                item.put("fichierUrl", support.getFichierUrl());
                item.put("ordre", support.getOrdre());

                supports.add(item);
            }
        }

        response.put("supports", supports);

        return response;
    }

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
    // UPDATE
    // =========================

    @Override
    @Transactional
    public Formation update(Long id, Formation request) {
        Formation formation = formationRepository.findByIdWithDetails(id);

        if (formation == null) {
            throw new RuntimeException("Formation introuvable");
        }

        formation.setTitre(request.getTitre());
        formation.setDescription(request.getDescription());
        formation.setDomaine(request.getDomaine());
        formation.setDureeHeures(request.getDureeHeures());

        if (request.getActif() != null) {
            formation.setActif(request.getActif());
        }

        if (formation.getVideos() != null) {
            formation.getVideos().clear();

            if (request.getVideos() != null) {
                int ordre = 1;

                for (FormationVideo video : request.getVideos()) {
                    video.setId(null);
                    video.setFormation(formation);

                    if (video.getOrdre() == 0) {
                        video.setOrdre(ordre++);
                    }

                    formation.getVideos().add(video);
                }
            }
        }

        if (formation.getSupports() != null) {
            formation.getSupports().clear();

            if (request.getSupports() != null) {
                int ordre = 1;

                for (FormationSupport support : request.getSupports()) {
                    support.setId(null);
                    support.setFormation(formation);

                    if (support.getOrdre() == 0) {
                        support.setOrdre(ordre++);
                    }

                    formation.getSupports().add(support);
                }
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
        Formation formation = formationRepository.findByIdWithDetails(id);

        if (formation == null) {
            throw new RuntimeException("Formation introuvable");
        }

        formation.setActif(true);
        formationRepository.save(formation);
    }

    @Override
    public void desactiver(Long id) {
        Formation formation = formationRepository.findByIdWithDetails(id);

        if (formation == null) {
            throw new RuntimeException("Formation introuvable");
        }

        formation.setActif(false);
        formationRepository.save(formation);
    }

    // =========================
    // FILTRES
    // =========================

    @Override
    public List<Formation> getActives() {
        return formationRepository.findByActifTrue();
    }

    @Override
    public List<Formation> getByDomaine(String domaine) {
        return formationRepository.findByDomaine(domaine);
    }

    // =========================
    // PARTICIPANTS
    // =========================

@Override
@Transactional(readOnly = true)
public List<EmployeDTO> getParticipants(Long formationId) {
    return employeRepository.findParticipantsByFormationId(formationId)
            .stream()
            .map(this::toEmployeDTO)
            .toList();
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
    // IA LOGIQUE 1 : GAP POSTE
    // =========================

@Override
@Transactional(readOnly = true)
public List<Formation> getRecommendationsAI(Long employeId) {
    try {
        Employe employe = getEmployeOrThrow(employeId);

        Map<String, Integer> userSkills = buildUserSkills(employeId);
        Map<String, Integer> requiredSkills = buildRequiredSkillsByPoste(employe.getPoste());

        System.out.println("===== RECO GAP POSTE SPRING =====");
        System.out.println("EMPLOYE ID = " + employeId);
        System.out.println("POSTE = " + employe.getPoste());
        System.out.println("USER SKILLS = " + userSkills);
        System.out.println("REQUIRED SKILLS = " + requiredSkills);
        System.out.println("=================================");

        if (userSkills.isEmpty()) {
            System.out.println("⚠️ Aucune compétence utilisateur trouvée");
            return List.of();
        }

        if (requiredSkills.isEmpty()) {
            System.out.println("⚠️ Aucune compétence requise trouvée pour le poste : " + employe.getPoste());
            return List.of();
        }

        List<Formation> formationsNonSuivies = getFormationsNonSuivies(employeId);

        if (formationsNonSuivies.isEmpty()) {
    System.out.println("⚠️ Aucune formation interne non suivie trouvée, l’IA utilisera les recommandations externes.");
}

      List<Map<String, Object>> formationsPayload = formationsNonSuivies.stream()
        .map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("titre", f.getTitre());
            map.put("description", f.getDescription());
            map.put("domaine", f.getDomaine());
            map.put("niveau", "");
            map.put("competences", List.of());
            return map;
        })
        .toList();

        List<String> formationsSuivies = buildFormationsSuivies(employeId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "GAP_POSTE");
        payload.put("poste", employe.getPoste());
        payload.put("userSkills", userSkills);
        payload.put("requiredSkills", requiredSkills);
       payload.put("formations", formationsPayload);
        payload.put("formationsSuivies", formationsSuivies);

        System.out.println("PAYLOAD GAP POSTE = " + payload);

        System.out.println("===== RECO GAP POSTE SPRING =====");
System.out.println("EMPLOYE ID = " + employeId);
System.out.println("POSTE = " + employe.getPoste());
System.out.println("USER SKILLS = " + userSkills);
System.out.println("REQUIRED SKILLS = " + requiredSkills);
System.out.println("=================================");
        return callFlaskAndMapToFormations(payload, formationsNonSuivies);

    } catch (Exception e) {
        System.err.println("❌ ERREUR getRecommendationsAI : " + e.getMessage());
        e.printStackTrace();
        return List.of();
    }
}

    // =========================
    // IA LOGIQUE 2 : BOOST COMPETENCES
    // =========================

   @Override
@Transactional(readOnly = true)
public List<Formation> getRecommendationsBySkills(Long employeId) {
        Employe employe = getEmployeOrThrow(employeId);

        Map<String, Integer> userSkills = buildUserSkills(employeId);

        if (userSkills.isEmpty()) {
            return List.of();
        }

        List<Formation> formationsNonSuivies = getFormationsNonSuivies(employeId);

        if (formationsNonSuivies.isEmpty()) {
            return List.of();
        }

     List<Map<String, Object>> formationsPayload = formationsNonSuivies.stream()
        .map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("titre", f.getTitre());
            map.put("description", f.getDescription());
            map.put("domaine", f.getDomaine());
            map.put("niveau", "");
            map.put("competences", List.of());
            return map;
        })
        .toList();

        List<String> formationsSuivies = buildFormationsSuivies(employeId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "BOOST_COMPETENCES");
        payload.put("poste", employe.getPoste());
        payload.put("userSkills", userSkills);
        payload.put("requiredSkills", Map.of());
        payload.put("formations", formationsPayload);
        payload.put("formationsSuivies", formationsSuivies);

        return callFlaskAndMapToFormations(payload, formationsNonSuivies);
    }

    // =========================
    // ALIASES ANCIENS
    // =========================

    @Override
    public List<Formation> getRecommendations(Long employeId) {
        return getRecommendationsAI(employeId);
    }

    @Override
    public List<Formation> recommander(Long employeId) {
        return getRecommendationsAI(employeId);
    }

    // =========================
    // RESET PROGRESS
    // =========================

    @Override
    public void resetProgress(Long formationId, Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe employe = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        EmployeFormation employeFormation = employeFormationRepository
                .findByEmploye_IdAndFormation_Id(employe.getId(), formationId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        employeFormation.setProgression(0);

        employeFormationRepository.save(employeFormation);
    }

    // =========================
    // HELPERS IA
    // =========================

    private Employe getEmployeOrThrow(Long employeId) {
        return employeRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable avec id: " + employeId));
    }

    private List<Formation> getFormationsNonSuivies(Long employeId) {
        List<Formation> formations = formationRepository.findFormationsNonSuivies(employeId);

        if (formations == null) {
            return List.of();
        }

        return formations.stream()
                .filter(Objects::nonNull)
                .filter(f -> Boolean.TRUE.equals(f.getActif()))
                .toList();
    }

    private Map<String, Integer> buildUserSkills(Long employeId) {
        List<EmployeCompetence> competencesEmploye =
                employeCompetenceRepository.findByEmploye_Id(employeId);

        Map<String, Integer> result = new LinkedHashMap<>();

        if (competencesEmploye == null) {
            return result;
        }

        for (EmployeCompetence ec : competencesEmploye) {
            if (ec.getCompetence() == null || ec.getCompetence().getNom() == null) {
                continue;
            }

            result.put(
                    ec.getCompetence().getNom(),
                    convertLevelToInt(ec.getNiveau())
            );
        }

        return result;
    }

private Map<String, Integer> buildRequiredSkillsByPoste(String poste) {
    Map<String, Integer> result = new LinkedHashMap<>();

    if (poste == null || poste.isBlank()) {
        System.out.println("⚠️ Poste employé vide");
        return result;
    }

    String cleanedPoste = poste.trim();

    List<PosteCompetence> competencesPoste =
            posteCompetenceRepository.findByPosteIgnoreCaseWithCompetence(cleanedPoste);

    if (competencesPoste == null || competencesPoste.isEmpty()) {
        competencesPoste =
                posteCompetenceRepository.findMatchingPosteWithCompetence(cleanedPoste);
    }

    System.out.println("===== DEBUG POSTE COMPETENCE =====");
    System.out.println("POSTE EMPLOYE = " + cleanedPoste);
    System.out.println("NB COMPETENCES POSTE = " + (competencesPoste != null ? competencesPoste.size() : 0));
    System.out.println("==================================");

    if (competencesPoste == null || competencesPoste.isEmpty()) {
        return result;
    }

    for (PosteCompetence pc : competencesPoste) {
        if (pc.getCompetence() == null || pc.getCompetence().getNom() == null) {
            continue;
        }

        result.put(
                pc.getCompetence().getNom(),
                convertLevelToInt(pc.getNiveauRequis())
        );
    }

    return result;
}
    private List<String> buildFormationsSuivies(Long employeId) {
        return employeFormationRepository.findByEmployeId(employeId)
                .stream()
                .map(EmployeFormation::getFormation)
                .filter(Objects::nonNull)
                .map(Formation::getTitre)
                .filter(Objects::nonNull)
                .toList();
    }

   @SuppressWarnings("unchecked")
private List<Formation> callFlaskAndMapToFormations(
        Map<String, Object> payload,
        List<Formation> formationsDisponibles
) {
    try {
       Map<String, Object> response = restTemplate.postForObject(
        aiRecommendationUrl + "/recommend",
        payload,
        Map.class
);

        if (response == null || response.isEmpty()) {
            return List.of();
        }

        Object recommendationsObject = response.get("recommendations");

        if (!(recommendationsObject instanceof List<?> recommendations)) {
            System.out.println("⚠️ Réponse IA sans recommendations : " + response);
            return List.of();
        }

        Map<String, Formation> formationByTitle = new LinkedHashMap<>();

        for (Formation formation : formationsDisponibles) {
            if (formation.getTitre() != null) {
                formationByTitle.put(formation.getTitre(), formation);
            }
        }

        List<Formation> result = new ArrayList<>();

        for (Object obj : recommendations) {
            if (!(obj instanceof Map<?, ?> item)) {
                continue;
            }

            Object titleObject = item.get("formation");

            if (titleObject == null) {
                titleObject = item.get("title");
            }

            if (titleObject == null) {
                continue;
            }

            Formation formation = formationByTitle.get(titleObject.toString());

            if (formation != null) {
                result.add(formation);
            }
        }

        return result.stream()
                .limit(6)
                .toList();

    } catch (Exception e) {
        System.err.println("❌ Erreur appel IA Flask : " + e.getMessage());
        e.printStackTrace();
        return List.of();
    }
}

    // =========================
    // HELPERS DTO
    // =========================

    private EmployeDTO toEmployeDTO(Employe employe) {
        if (employe == null) {
            return null;
        }

        String photo = employe.getPhotoUrl();

        return EmployeDTO.builder()
                .id(employe.getId())
                .matricule(employe.getMatricule())
                .nom(employe.getNom())
                .prenom(employe.getPrenom())
                .email(employe.getEmail())
                .telephone(employe.getTelephone())
                .poste(employe.getPoste())
                .departement(employe.getDepartement())
                .statut(employe.getStatut())
                .dateEmbauche(employe.getDateEmbauche())
                .soldeConges(employe.getSoldeConges())
                .photoUrl(photo)
                .employePhotoProfil(photo)
                .build();
    }

    private int convertLevelToInt(String niveau) {
        if (niveau == null || niveau.isBlank()) {
            return 0;
        }

        return switch (niveau.trim().toUpperCase()) {
            case "DEBUTANT", "DÉBUTANT" -> 1;
            case "INTERMEDIAIRE", "INTERMÉDIAIRE" -> 2;
            case "AVANCE", "AVANCÉ" -> 3;
            case "EXPERT" -> 4;
            case "MAITRISE", "MAÎTRISE" -> 5;
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
        return niveau != null ? niveau : 0;
    }


    @Override
@Transactional
public void retirerParticipant(Long formationId, Long employeId) {

    Formation formation = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation introuvable"));

    Employe employe = employeRepository.findById(employeId)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    EmployeFormation employeFormation =
            employeFormationRepository
                    .findByEmploye_IdAndFormation_Id(employeId, formationId)
                    .orElseThrow(() ->
                            new RuntimeException("Inscription employé-formation introuvable"));

    employeFormationRepository.delete(employeFormation);

}
}