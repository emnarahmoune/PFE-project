// package com.codeWithProject.ecom.service;

// import com.codeWithProject.ecom.entity.Employe;
// import com.codeWithProject.ecom.entity.Competence;
// import com.codeWithProject.ecom.repository.EmployeRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;
// import com.codeWithProject.ecom.entity.EmployeCompetence;
// import com.codeWithProject.ecom.entity.Formation;
// import com.codeWithProject.ecom.repository.EmployeCompetenceRepository;
// import com.codeWithProject.ecom.repository.FormationRepository;
// import java.util.*;

// @Service
// @RequiredArgsConstructor
// public class RecommendationService {

//     private final EmployeRepository employeRepository;
//     private final RestTemplate restTemplate;
//     private final EmployeCompetenceRepository employeCompetenceRepository;
//     private final FormationRepository formationRepository;


//     public List<Map<String, Object>> recommend(Long userId) {

//         Employe emp = employeRepository.findById(userId).orElseThrow();

//         List<Integer> vector = buildVector(emp);

//         Map<String, Object> payload = new HashMap<>();
//         payload.put("competences", vector);

//         String url = "http://localhost:8000/recommend";

//         return restTemplate.postForObject(url, payload, List.class);
//     }

//     public List<Formation> recommander(Long employeId) {

//     List<EmployeCompetence> competences =
//             employeCompetenceRepository.findByEmployeId(employeId);

//     List<String> domaines = competences.stream()
//             .map(c -> c.getCompetence().getCategorie())
//             .distinct()
//             .toList();

//     return formationRepository.findFormationsRecommandees(domaines, employeId);
// }

//     private List<Integer> buildVector(Employe emp) {

//         int java = 0;
//         int communication = 0;
//         int management = 0;

//         for (EmployeCompetence ec : emp.getCompetences()) {

//             switch (ec.getCompetence().getNom().toLowerCase()) {
//                 case "java" -> java = levelToInt(ec.getNiveau());
//                 case "communication" -> communication = levelToInt(ec.getNiveau());
//                 case "management" -> management = levelToInt(ec.getNiveau());
//             }
//         }

//         return List.of(java, communication, management);
//     }

//     private int levelToInt(String niveau) {
//         return switch (niveau) {
//             case "DEBUTANT" -> 1;
//             case "INTERMEDIAIRE" -> 2;
//             case "AVANCE" -> 3;
//             case "EXPERT" -> 5;
//             default -> 1;
//         };
//     }
// }