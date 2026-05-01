package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.FormationService;
import com.codeWithProject.ecom.service.dto.EmployeDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FormationController {

    private final FormationService formationService;
    private final EmployeRepository employeRepository;

    // =========================
    // 🔥 RECOMMANDATION SKILL
    // =========================
    @GetMapping("/recommandations-skill")
    public List<Formation> getRecoSkill(Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        return formationService.getRecommendationsBySkills(emp.getId());
    }

    // =========================
    // CRUD
    // =========================

    @PostMapping
    public Formation create(@RequestBody Formation formation) {
        return formationService.create(formation);
    }

   @GetMapping
   public List<Map<String, Object>> getAll() {
    return formationService.getAll();
}

    @GetMapping("/{id}")
public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(formationService.getByIdComplete(id));
}

    @PutMapping("/id/{id}")
    public Formation update(@PathVariable Long id, @RequestBody Formation formation) {
        return formationService.update(id, formation);
    }

    @DeleteMapping("/id/{id}")
    public void delete(@PathVariable Long id) {
        formationService.delete(id);
    }

    // =========================
    // 👥 PARTICIPANTS
    // =========================
    @GetMapping("/{id}/participants")
public List<EmployeDTO> getParticipants(@PathVariable Long id) {
    return formationService.getParticipants(id);
}

    // =========================
    // 📊 STATS
    // =========================
@GetMapping("/stats")
public Map<String, Object> stats() {

    Map<String, Object> stats = new HashMap<>();

   List<Map<String, Object>> formations = formationService.getAll();

    stats.put("total", formations.size());
    stats.put("actives", formationService.getActives().size());

    int totalParticipants = formations.stream()
            .mapToInt(f -> formationService.getParticipants((Long) f.get("id")).size())
            .sum();

    double dureeMoyenne = formations.stream()
    .mapToInt(f -> (int) f.get("dureeHeures"))
    .average()
    .orElse(0);

    stats.put("totalParticipants", totalParticipants);
    stats.put("dureeMoyenne", dureeMoyenne);

    return stats;
}
    // =========================
    // 🔎 FILTRES
    // =========================
    @GetMapping("/domaine/{domaine}")
    public List<Formation> getByDomaine(@PathVariable String domaine) {
        return formationService.getByDomaine(domaine);
    }

    @GetMapping("/actives")
    public List<Formation> getActives() {
        return formationService.getActives();
    }




@GetMapping("/{id}/details")
public ResponseEntity<Map<String, Object>> getDetails(@PathVariable Long id) {

    Map<String, Object> data = new HashMap<>(formationService.getByIdComplete(id));

    List<EmployeDTO> participants = formationService.getParticipants(id);

    data.put("employes", participants);
    data.put("nombreParticipants", participants.size());

    return ResponseEntity.ok(data);
}



@PutMapping("/{id}/activer")
public void activer(@PathVariable Long id) {
    formationService.activer(id);
}

@PutMapping("/{id}/desactiver")
public void desactiver(@PathVariable Long id) {
    formationService.desactiver(id);
}
    // =========================
    // 👤 FORMATIONS UTILISATEUR
    // =========================
    @GetMapping("/me")
    public List<Formation> getMyFormations(Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        return formationService.getFormationsByEmploye(emp.getId());
    }

    // =========================
    // 🤖 RECOMMANDATION IA
    // =========================
    @GetMapping("/recommandations")
    public List<Formation> getRecommandations(Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        return formationService.getRecommendationsAI(emp.getId());
    }

    // =========================
    // 📄 UPLOAD PDF
    // =========================
    @PostMapping("/upload-pdf")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {

        try {
            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body("Seuls les fichiers PDF sont autorisés");
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get("uploads/" + fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return ResponseEntity.ok(Map.of("filePath", fileName));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur upload PDF");
        }
    }
}