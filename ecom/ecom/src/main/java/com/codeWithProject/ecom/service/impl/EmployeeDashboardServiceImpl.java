package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.repository.EmployeCompetenceRepository;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.EvaluationRepository;
import com.codeWithProject.ecom.repository.FormationRecommendationRepository;
import com.codeWithProject.ecom.service.EmployeeDashboardService;
import com.codeWithProject.ecom.service.dto.EmployeeDashboardStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
@Service
@RequiredArgsConstructor
public class EmployeeDashboardServiceImpl implements EmployeeDashboardService {

    private final EmployeRepository employeRepository;
    private final EmployeFormationRepository employeFormationRepository;
    private final EvaluationRepository evaluationRepository;
    private final FormationRecommendationRepository formationRecommendationRepository;

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeCompetenceRepository employeCompetenceRepository;
    @Override
    public EmployeeDashboardStatsDTO getMesStats() {
        Employe employe = getEmployeConnecte();
        Long employeId = employe.getId();

        List<EmployeFormation> formations =
                employeFormationRepository.findByEmployeId(employeId);

        int formationsTerminees = calculerFormationsTerminees(formations);
        double progressionFormations = calculerProgressionFormations(formations);

        double noteMoyenne = getNoteMoyenne(employeId);

        int recommandationsIA = getRecommandationsIA(employeId);

        int soldeInitial = employe.getSoldeConges() == null
        ? 0
        : employe.getSoldeConges().intValue();

int joursCongesPris = getJoursCongesPris(employeId);

int congesRestants = Math.max(soldeInitial - joursCongesPris, 0);

        return EmployeeDashboardStatsDTO.builder()
                .congesRestants(congesRestants)
                .formationsTerminees(formationsTerminees)
                .progressionFormations(progressionFormations)
                .competencesValidees(getCompetencesValidees(employeId))
                .noteMoyenne(noteMoyenne)
                .certificatsObtenus(formationsTerminees)
                .recommandationsIA(recommandationsIA)
                .build();
    }




private int getJoursCongesPris(Long employeId) {
    try {
        return demandeCongeRepository.findByEmployeId(employeId)
                .stream()
                .filter(conge ->
                        "APPROUVE".equalsIgnoreCase(String.valueOf(conge.getStatut()))
                                || "APPROUVÉ".equalsIgnoreCase(String.valueOf(conge.getStatut()))
                                || "APPROVED".equalsIgnoreCase(String.valueOf(conge.getStatut()))
                                || "ACCEPTE".equalsIgnoreCase(String.valueOf(conge.getStatut()))
                                || "ACCEPTÉ".equalsIgnoreCase(String.valueOf(conge.getStatut()))
                )
                .mapToInt(conge -> {
                    if (conge.getDateDebut() == null || conge.getDateFin() == null) {
                        return 0;
                    }

                    long jours = ChronoUnit.DAYS.between(
                            conge.getDateDebut(),
                            conge.getDateFin()
                    ) + 1;

                    return (int) Math.max(jours, 0);
                })
                .sum();
    } catch (Exception e) {
        return 0;
    }
}




    private int getCompetencesValidees(Long employeId) {
    try {
        return employeCompetenceRepository.findByEmployeId(employeId).size();
    } catch (Exception e) {
        return 0;
    }
}

 private Employe getEmployeConnecte() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
        throw new RuntimeException("Utilisateur non authentifié");
    }

    String extractedEmail = null;

    if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
        extractedEmail = jwt.getClaimAsString("email");
    }

    if (extractedEmail == null || extractedEmail.isBlank()) {
        extractedEmail = authentication.getName();
    }

    final String email = extractedEmail;

    return employeRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Employé connecté introuvable : " + email));
}
    private int calculerFormationsTerminees(List<EmployeFormation> formations) {
        if (formations == null || formations.isEmpty()) {
            return 0;
        }

        return (int) formations.stream()
                .filter(f ->
                        "TERMINEE".equalsIgnoreCase(String.valueOf(f.getStatut()))
                                || "TERMINE".equalsIgnoreCase(String.valueOf(f.getStatut()))
                                || (f.getProgression() != null && f.getProgression() >= 100)
                )
                .count();
    }

    private double calculerProgressionFormations(List<EmployeFormation> formations) {
        if (formations == null || formations.isEmpty()) {
            return 0.0;
        }

        double moyenne = formations.stream()
                .mapToDouble(f -> f.getProgression() == null ? 0.0 : f.getProgression())
                .average()
                .orElse(0.0);

        return Math.round(moyenne * 100.0) / 100.0;
    }

    private double getNoteMoyenne(Long employeId) {
        try {
            Double moyenne = evaluationRepository.moyenneEvaluationAnnuelle(
                    employeId,
                    java.time.LocalDate.now().getYear()
            );

            return moyenne == null ? 0.0 : Math.round(moyenne * 100.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getRecommandationsIA(Long employeId) {
        try {
            return formationRecommendationRepository.findByEmployeId(employeId).size();
        } catch (Exception e) {
            return 0;
        }
    }
}