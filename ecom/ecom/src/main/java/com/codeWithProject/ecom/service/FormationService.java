package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Formation;
import java.util.List;

public interface FormationService {

    Formation create(Formation formation);

    List<Formation> getAll();

    Formation getById(Long id);

    Formation update(Long id, Formation formation);

    void delete(Long id);

    List<Formation> getByDomaine(String domaine);

    List<Formation> getActives();
List<Formation> recommander(Long employeId);
List<Formation> getFormationsByEmploye(Long employeId);

List<Formation> getRecommendationsAI(Long employeId);
List<Formation> getRecommendations(Long employeId);
List<Formation> getRecommendationsBySkills(Long employeId);
}
