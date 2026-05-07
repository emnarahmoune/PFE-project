package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import org.springframework.security.core.Authentication;


public interface FormationService {

    Formation create(Formation formation);

    List<Map<String, Object>> getAll();

    Formation getById(Long id);

    Formation update(Long id, Formation formation);

    void delete(Long id);

    List<Formation> getByDomaine(String domaine);

    List<Formation> getActives();

    List<Formation> recommander(Long employeId);

    List<Formation> getFormationsByEmploye(Long employeId);

    List<Formation> getRecommendations(Long employeId);

    List<Formation> getRecommendationsBySkills(Long employeId);

    List<Formation> getRecommendationsAI(Long employeId);

    List<EmployeDTO> getParticipants(Long formationId);

Map<String, Object> getByIdWithEmployes(Long id);

Map<String, Object> getByIdComplete(Long id);


void activer(Long id);
void desactiver(Long id);


void resetProgress(Long formationId, Authentication auth);


}