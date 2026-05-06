package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.EmployeCompetence;
import com.codeWithProject.ecom.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
 import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface EmployeService {

    List<EmployeDTO> findAll();
    Page<EmployeDTO> findAll(Pageable pageable);
    long count();


    void addCompetence(Long userId, Long compId, int niveau);

    Optional<EmployeDTO> findById(Long id);
    Optional<EmployeDTO> findByMatricule(String matricule);
    List<EmployeDTO> findByDepartement(String departement);
    List<EmployeDTO> findByStatut(String statut);
    List<EmployeDTO> findByManagerId(Long managerId);
    List<EmployeDTO> findByServiceId(Long serviceId);
    List<EmployeDTO> findActifs();
    List<EmployeDTO> findSoldeCongesFaible(Integer seuil);

    EmployeDTO create(EmployeDTO dto);
    EmployeDTO update(Long id, EmployeDTO dto);
    EmployeDTO mettreAJourProfil(Long id, String poste, Double salaire, String departement);
    EmployeDTO changerStatut(Long id, String nouveauStatut);
    void delete(Long id);

    Map<String, Long> countByDepartement();
    Map<String, Long> countByStatut();
    Double calculerMasseSalariale();
    Double calculerSalaireMoyen();
    List<EmployeDTO> findEmployesRecents(int limit);
    List<EmployeDTO> search(String keyword);
    TableauBordEmployeDTO getStatsTableauBord();

    Optional<EmployeDTO> findByEmail(String email);
    SoldeCongesDTO getSoldeCongesByEmail(String email);
    List<CompetenceEmployeDTO> getCompetencesByEmail(String email);
    List<FormationEmployeDTO> getFormationsByEmail(String email);
    List<HistoriqueCongeDTO> getHistoriqueCongesByEmail(String email);
    EmployeDTO updateProfilByEmail(String email, UpdateProfilRequest request);
    void changePassword(Jwt jwt, ChangePasswordRequest dto);
    List<EmployeCompetence> getCompetencesEntity(Long userId);
    EmployeDTO changeEmailByEmail(String email, String newEmail);
    Long getEmployeIdByEmail(String email);

    void updateCompetence(Long userId, Long compId, int niveau);
    List<FormationEmployeDTO> getFormationsByEmployeId(Long employeId);

    List<EmployeDTO> findAllManagers();
    List<EmployeDTO> getEquipeByManagerEmail(String managerEmail);
    EmployeDTO getEmployeForManager(Long employeId, String managerEmail);
    EmployeDTO updateManager(Long employeId, Long managerId);

   String uploadPhotoProfilByEmail(String email, MultipartFile file) throws IOException;

void deletePhotoProfilByEmail(String email);
   
   

List<EmployeDTO> getManagers();



List<HistoriqueCongeDTO> getCongesByEmployeId(Long employeId);
List<EvaluationDTO> getEvaluationsByEmployeId(Long employeId);

void updateCompetences(Long employeId, List<CompetenceDTO> dtos);
}