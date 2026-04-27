package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
import com.codeWithProject.ecom.service.FormationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationServiceImpl implements FormationService {

    private final FormationRepository formationRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final PosteCompetenceRepository posteCompetenceRepository;
    private final FormationCompetenceRepository formationCompetenceRepository;
    private final EmployeRepository employeRepository;

    // 🔥 CREATE
    @Override
    public Formation create(Formation formation) {
        return formationRepository.save(formation);
    }

    // 🔍 GET ALL
    @Override
    public List<Formation> getAll() {
        return formationRepository.findAll();
    }

    // 🔍 GET BY ID
    @Override
    public Formation getById(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
    }

    // 🔄 UPDATE
    @Override
    public Formation update(Long id, Formation f) {
        Formation formation = getById(id);

        formation.setTitre(f.getTitre());
        formation.setDescription(f.getDescription());
        formation.setDomaine(f.getDomaine());
        formation.setDureeHeures(f.getDureeHeures());
        formation.setActif(f.getActif());

        return formationRepository.save(formation);
    }

    // ❌ DELETE
    @Override
    public void delete(Long id) {
        formationRepository.deleteById(id);
    }

    // 🔍 FILTER
    @Override
    public List<Formation> getByDomaine(String domaine) {
        return formationRepository.findByDomaine(domaine);
    }

    @Override
    public List<Formation> getActives() {
        return formationRepository.findByActifTrue();
    }

    // 🧠 RECOMMANDATION PAR GAP (niveau)

    @Override
public List<Formation> recommander(Long employeId) {

    // 🔹 récupérer employé
    Employe emp = employeRepository.findById(employeId)
            .orElseThrow();

    String poste = emp.getPoste();

    List<EmployeCompetence> userSkills =
            employeCompetenceRepository.findByEmployeId(employeId);

    List<PosteCompetence> jobSkills =
            posteCompetenceRepository.findByPoste(poste);

    Map<Long, Integer> niveauxUser = new HashMap<>();

    for (EmployeCompetence c : userSkills) {
        niveauxUser.put(c.getCompetence().getId(), c.getNiveau());
    }

    Map<Long, Integer> gaps = new HashMap<>();

    for (PosteCompetence pc : jobSkills) {

        int niveauUser = niveauxUser.getOrDefault(
                pc.getCompetence().getId(), 0);

        int gap = pc.getNiveauRequis() - niveauUser;

        if (gap > 0) {
            gaps.put(pc.getCompetence().getId(), gap);
        }
    }

    Map<Formation, Integer> scores = new HashMap<>();

    for (Map.Entry<Long, Integer> entry : gaps.entrySet()) {

        Long competenceId = entry.getKey();
        int gap = entry.getValue();

        List<FormationCompetence> list =
                formationCompetenceRepository.findByCompetence_Id(competenceId);

        for (FormationCompetence fc : list) {

            Formation f = formationRepository
                    .findById(fc.getFormationId())
                    .orElse(null);

            if (f == null) continue;

            scores.put(
                f,
                scores.getOrDefault(f, 0) + gap
            );
        }
    }

    return scores.entrySet()
            .stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .map(Map.Entry::getKey)
            .limit(3)
            .toList();
}

    // 🔹 MES FORMATIONS
    @Override
    public List<Formation> getFormationsByEmploye(Long employeId) {

        return employeFormationRepository.findByEmployeId(employeId)
                .stream()
                .map(ef -> ef.getFormation())
                .toList();
    }

    // 🔥 RECO (sans IA)
    public List<Formation> getRecommendations(Long employeId) {

        Employe emp = employeRepository.findById(employeId)
                .orElseThrow();

       List<Formation> reco = recommander(employeId);

        List<Long> dejaInscrit = employeFormationRepository.findByEmployeId(employeId)
                .stream()
                .map(ef -> ef.getFormation().getId())
                .toList();

        return reco.stream()
                .filter(f -> !dejaInscrit.contains(f.getId()))
                .toList();
    }

    // 🤖 RECOMMANDATION IA (Flask)
 @Override
public List<Formation> getRecommendationsAI(Long employeId) {

    // 🔹 récupérer employé
    Employe emp = employeRepository.findById(employeId)
            .orElseThrow();

    // 🔹 récupérer formations non suivies
    List<Formation> formations =
            formationRepository.findFormationsNonSuivies(employeId);

    if (formations.isEmpty()) {
        return new ArrayList<>();
    }

    // 🔹 compétences utilisateur
    List<EmployeCompetence> userSkillsList =
            employeCompetenceRepository.findByEmployeId(employeId);

    Map<String, Integer> userSkills = new HashMap<>();

    for (EmployeCompetence ec : userSkillsList) {

        if (ec.getCompetence() != null && ec.getCompetence().getNom() != null) {

            userSkills.put(
    ec.getCompetence().getNom().toLowerCase(),
    convertNiveau(ec.getNiveau())
);
        }
    }

    // 🔹 compétences requises poste
    List<PosteCompetence> jobSkillsList =
            posteCompetenceRepository.findByPoste(emp.getPoste());

    Map<String, Integer> requiredSkills = new HashMap<>();

    for (PosteCompetence pc : jobSkillsList) {

        if (pc.getCompetence() != null && pc.getCompetence().getNom() != null) {

           try {
   requiredSkills.put(
    pc.getCompetence().getNom().toLowerCase(),
    pc.getNiveauRequis()
);
} catch (Exception e) {
    System.out.println("❌ ERREUR conversion niveau: " + pc.getNiveauRequis());
}
        }
    }

    // 🔥 DEBUG
    System.out.println("POSTE = " + emp.getPoste());
    System.out.println("USER SKILLS = " + userSkills);
    System.out.println("REQUIRED SKILLS = " + requiredSkills);

    // 🔥 sécurité si aucun mapping poste
    if (requiredSkills.isEmpty()) {
        System.out.println("⚠️ Aucune compétence trouvée pour ce poste !");
        return formations.stream().limit(3).toList();
    }

    // 🔹 liste noms formations
    List<String> formationNames = formations.stream()
            .map(f -> f.getTitre().toLowerCase())
            .toList();

    // 🔹 appel Flask
    Map<String, Object> body = new HashMap<>();
    body.put("userSkills", userSkills);
    body.put("requiredSkills", requiredSkills);
    body.put("formations", formationNames);
    body.put("formationsSuivies", new ArrayList<>());

    RestTemplate restTemplate = new RestTemplate();

Object rawResponse = restTemplate.postForObject(
    "http://localhost:5000/recommend",
    body,
    Object.class
);

if (!(rawResponse instanceof List<?> list)) {
    return formations.stream().limit(3).toList();
}

List<Map<String, Object>> response = (List<Map<String, Object>>) list;
    try {
        response = restTemplate.postForObject(
                "http://localhost:5000/recommend",
                body,
                List.class
        );
    } catch (Exception e) {
        System.out.println("❌ ERREUR FLASK: " + e.getMessage());
        return formations.stream().limit(3).toList();
    }

    // 🔥 sécurité réponse
    if (response == null || response.isEmpty()) {
        return formations.stream().limit(3).toList();
    }

    // 🔹 récupérer noms recommandés
    List<String> recommendedNames = response.stream()
            .map(r -> ((String) r.get("formation")).toLowerCase())
            .toList();

    // 🔹 filtrer formations
    return formations.stream()
            .filter(f -> recommendedNames.contains(f.getTitre().toLowerCase()))
            .toList();
}



private int convertNiveau(String niveau) {
    if (niveau == null) return 0;

    return switch (niveau.toUpperCase()) {
        case "DEBUTANT" -> 1;
        case "INTERMEDIAIRE" -> 2;
        case "AVANCE" -> 3;
        case "EXPERT" -> 4;
        default -> 0;
    };
}


@Override
public List<Formation> getRecommendationsBySkills(Long employeId) {

    // 🔹 compétences utilisateur
    List<EmployeCompetence> userSkills =
            employeCompetenceRepository.findByEmployeId(employeId);

    // 🔹 formations non suivies
    List<Formation> formations =
            formationRepository.findFormationsNonSuivies(employeId);

    Map<Formation, Integer> scores = new HashMap<>();

    for (EmployeCompetence ec : userSkills) {

        String skill = ec.getCompetence().getNom().toLowerCase();
        int niveauUser = convertNiveau(ec.getNiveau());

        // 🔥 gap = progression
        int scoreBase = 5 - niveauUser; // plus faible = plus prioritaire

        for (Formation f : formations) {

            if (f.getTitre().toLowerCase().contains(skill)) {

                scores.put(
                    f,
                    scores.getOrDefault(f, 0) + scoreBase
                );
            }
        }
    }

    return scores.entrySet()
            .stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .map(Map.Entry::getKey)
            .limit(3)
            .toList();
}
}