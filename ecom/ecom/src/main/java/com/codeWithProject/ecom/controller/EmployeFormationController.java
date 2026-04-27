package com.codeWithProject.ecom.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.codeWithProject.ecom.entity.FormationVideo;
import com.codeWithProject.ecom.service.EmployeFormationService;
import com.codeWithProject.ecom.repository.EmployeFormationProgressRepository;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.codeWithProject.ecom.entity.Employe;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/employe-formations")
public class EmployeFormationController {

    private final EmployeFormationService service;
    private final EmployeRepository employeRepository;
    private final EmployeFormationRepository efRepository;
    private EmployeFormationProgressRepository progressRepository;
    public EmployeFormationController(
        EmployeFormationService service,
        EmployeRepository employeRepository,
        EmployeFormationRepository efRepository,EmployeFormationProgressRepository progressRepository) {

    this.service = service;
    this.employeRepository = employeRepository;
    this.efRepository = efRepository;
    this.progressRepository = progressRepository;
}

    // 🔥 INSCRIPTION
@PostMapping("/inscrire")
public ResponseEntity<?> inscrire(@RequestBody Map<String, Long> body,
                                  Authentication authentication) {

    if (authentication == null) {
        throw new RuntimeException("Utilisateur non authentifié");
    }

    Jwt jwt = (Jwt) authentication.getPrincipal();

    String email = jwt.getClaimAsString("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    Long formationId = body.get("formationId");

    service.inscrire(emp.getId(), formationId);

    return ResponseEntity.ok(Map.of("message", "Inscription réussie"));}

    // 🔥 VIDEOS
    @GetMapping("/{id}/videos")
    public List<FormationVideo> videos(@PathVariable Long id) {
        return service.getVideos(id);
    }

   
@GetMapping("/mes-formations")
public List<?> getMesFormations(Authentication auth) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaimAsString("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow();

    return efRepository.findByEmployeId(emp.getId())
            .stream()
            .map(ef -> Map.of(
                    "id", ef.getId(),
                    "formation", Map.of(
                            "id", ef.getFormation().getId(),   // 🔥 CRITIQUE
                            "titre", ef.getFormation().getTitre(),
                            "domaine", ef.getFormation().getDomaine()
                    ),
                    "progression", ef.getProgression()
            ))
            .toList();
}

    // 🔥 COMPLETER VIDEO
@PostMapping("/complete-video/{videoId}")
public ResponseEntity<?> complete(@PathVariable Long videoId,
                                  @AuthenticationPrincipal Jwt jwt) {

    String email = jwt.getClaimAsString("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    service.completeVideo(emp.getId(), videoId);

    return ResponseEntity.ok(Map.of("message", "progress updated"));}



    @GetMapping("/{formationId}/completed-videos")
public List<Long> getCompletedVideos(Authentication auth,
                                     @PathVariable Long formationId) {

    Jwt jwt = (Jwt) auth.getPrincipal();
    String email = jwt.getClaimAsString("email");

    Employe emp = employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

    return progressRepository
            .findByEmployeIdAndVideo_Formation_IdAndCompletedTrue(
                    emp.getId(), formationId)
            .stream()
            .map(p -> p.getVideo().getId())
            .toList();
}

}