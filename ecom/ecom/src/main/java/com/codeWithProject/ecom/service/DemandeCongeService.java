package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.DemandeCongeDTO;
import com.codeWithProject.ecom.service.dto.SoldeCongesDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DemandeCongeService {

    // ===== MÉTHODES DE BASE =====
    List<DemandeCongeDTO> findAll();
    Page<DemandeCongeDTO> findAll(Pageable pageable);
    Optional<DemandeCongeDTO> findById(Long id);
    List<DemandeCongeDTO> findByEmployeId(Long employeId);
    List<DemandeCongeDTO> findByManagerId(Long managerId);
    List<DemandeCongeDTO> findByStatut(String statut);
    List<DemandeCongeDTO> findByType(String type);
    List<DemandeCongeDTO> findDemandesUrgentes();
    List<DemandeCongeDTO> findUrgentesEnAttente();
    List<DemandeCongeDTO> findDemandesEnAttentePourManager(Long managerId);
    List<DemandeCongeDTO> findCongesEnCours();

    // ===== MÉTHODES CRUD =====
    DemandeCongeDTO create(DemandeCongeDTO dto);
    DemandeCongeDTO modifier(Long id, DemandeCongeDTO dto);
    DemandeCongeDTO annuler(Long id);
    DemandeCongeDTO valider(Long id, Long managerId);
    DemandeCongeDTO refuser(Long id, Long managerId, String motif);
    void delete(Long id);

    // ===== MÉTHODES STATISTIQUES =====
    Map<String, Long> countByStatut();
    Map<String, Long> countByType();
    Map<Integer, Long> getStatsMensuelles(int annee);
    boolean hasConflitDates(Long employeId, LocalDate debut, LocalDate fin, Long demandeId);

    // ===== MÉTHODES POUR L'UTILISATEUR AUTHENTIFIÉ =====
    DemandeCongeDTO createForAuthenticatedUser(DemandeCongeDTO dto, String email);
    List<DemandeCongeDTO> findByEmployeEmail(String email);
    SoldeCongesDTO getSoldeCongesByEmail(String email);
    DemandeCongeDTO modifierForAuthenticatedUser(Long id, DemandeCongeDTO dto, String email);
    DemandeCongeDTO annulerForAuthenticatedUser(Long id, String email);
    List<DemandeCongeDTO> getCongesByEmployeIdForManager(Long employeId, String managerEmail);
}
