package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IndicateurRHService {

    List<IndicateurRHDTO> findAll();
    Page<IndicateurRHDTO> findAll(Pageable pageable);
    Optional<IndicateurRHDTO> findById(Long id);
    List<IndicateurRHDTO> findByType(String type);
    List<IndicateurRHDTO> findByPeriode(String periode);
    List<IndicateurRHDTO> findByDepartement(String departement);
    Optional<IndicateurRHDTO> findDernierIndicateurByType(String type);
    List<IndicateurRHDTO> findIndicateursRecents(int jours);
    List<IndicateurRHDTO> findAvecAlerte();
    IndicateurRHDTO create(IndicateurRHDTO dto);
    IndicateurRHDTO calculerTurnover(LocalDate dateDebut, LocalDate dateFin, String periode, String departement);

    // Méthode pour l'absentéisme global (entreprise/département)
    IndicateurRHDTO calculerAbsenteisme(LocalDate dateDebut, LocalDate dateFin, String periode, String departement);

    // 🔥 Nouvelle méthode pour l'absentéisme d'un employé spécifique
    IndicateurRHDTO calculerAbsenteisme(LocalDate dateDebut, LocalDate dateFin, String periode, String departement, Long employeId);

    IndicateurRHDTO calculerPerformance(LocalDate dateCalcul, String periode, String departement);
    IndicateurRHDTO calculerSatisfaction(LocalDate dateCalcul, String periode, String departement);
    IndicateurRHDTO calculerCouvertureCompetences(LocalDate dateCalcul, String periode, String departement);
    IndicateurRHDTO update(Long id, IndicateurRHDTO dto);
    void delete(Long id);
    Map<String, Double> getMoyennesByType();
    List<IndicateurRHDTO> getHistoriqueIndicateur(String type, int limite);
    List<IndicateurRHDTO> findEnHausse(String type);
    List<IndicateurRHDTO> findEnBaisse(String type);
    Map<String, IndicateurRHDTO> getDerniersIndicateurs();
    Map<String, Object> getStatsTableauBord();
}