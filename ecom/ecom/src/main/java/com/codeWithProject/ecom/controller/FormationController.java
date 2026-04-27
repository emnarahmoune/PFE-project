package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.FormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

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
    public List<Formation> getAll() {
        return formationService.getAll();
    }

    // 🔥 FIX ICI
    @GetMapping("/id/{id}")
    public Formation getById(@PathVariable Long id) {
        return formationService.getById(id);
    }

    @PutMapping("/id/{id}")
    public Formation update(@PathVariable Long id, @RequestBody Formation formation) {
        return formationService.update(id, formation);
    }

    @DeleteMapping("/id/{id}")
    public void delete(@PathVariable Long id) {
        formationService.delete(id);
    }

    @GetMapping("/domaine/{domaine}")
    public List<Formation> getByDomaine(@PathVariable String domaine) {
        return formationService.getByDomaine(domaine);
    }

    @GetMapping("/actives")
    public List<Formation> getActives() {
        return formationService.getActives();
    }

    @GetMapping("/me")
    public List<Formation> getMyFormations(Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow();

        return formationService.getFormationsByEmploye(emp.getId());
    }

    @GetMapping("/recommandations")
    public List<Formation> getRecommandations(Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow();

        return formationService.getRecommendationsAI(emp.getId());
    }
}