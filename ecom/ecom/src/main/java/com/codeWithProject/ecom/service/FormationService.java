package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface FormationService {

    Formation create(Formation formation);

    List<Map<String, Object>> getAll();

    Formation getById(Long id);

    Map<String, Object> getByIdComplete(Long id);

    Map<String, Object> getByIdWithEmployes(Long id);

    Formation update(Long id, Formation formation);

    void delete(Long id);

    void activer(Long id);

    void desactiver(Long id);

    List<Formation> getByDomaine(String domaine);

    List<Formation> getActives();

    List<EmployeDTO> getParticipants(Long formationId);

    List<Formation> getFormationsByEmploye(Long employeId);

    /*
     * IA - logique 1 :
     * poste -> compétences requises -> gaps -> formations recommandées
     */
    List<Formation> getRecommendationsAI(Long employeId);

    /*
     * IA - logique 2 :
     * compétences actuelles -> formations pour booster ces compétences
     */
    List<Formation> getRecommendationsBySkills(Long employeId);

    /*
     * Alias pour compatibilité ancienne logique.
     */
    List<Formation> getRecommendations(Long employeId);

    /*
     * Alias pour compatibilité ancienne logique.
     */
    List<Formation> recommander(Long employeId);

    void resetProgress(Long formationId, Authentication auth);
}