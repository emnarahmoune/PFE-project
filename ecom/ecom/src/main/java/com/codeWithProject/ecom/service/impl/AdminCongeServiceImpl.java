package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.repository.DemandeCongeRepository;
import com.codeWithProject.ecom.service.AdminCongeService;
import com.codeWithProject.ecom.service.WorkflowService;
import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCongeServiceImpl implements AdminCongeService {

    private final DemandeCongeRepository demandeRepository;
    private final WorkflowService workflowService;

    @Override
    public List<DemandeCongeAdminDTO> getDemandesEnAttentePlusDe10Jours() {
        // Cette méthode peut être utilisée pour une consultation directe en base
        List<DemandeConge> demandes = demandeRepository.findByStatutAndJoursOuvresGreaterThan("EN_ATTENTE", 10);
        return demandes.stream().map(DemandeCongeAdminDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DemandeCongeAdminDTO> getAllDemandes() {
        return demandeRepository.findAll().stream()
                .map(DemandeCongeAdminDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public DemandeCongeAdminDTO getDemandeById(Long id) {
        DemandeConge demande = demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));
        return DemandeCongeAdminDTO.fromEntity(demande);
    }

    @Override
    @Transactional
    public void validerDemande(Long demandeId, String commentaire, String adminEmail) {
        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être approuvées");
        }

        String processInstanceId = demande.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new BusinessException("Demande sans instance Camunda, impossible de valider via workflow");
        }

        List<Map<String, Object>> tasks = workflowService.getRHTasks(adminEmail);
        Map<String, Object> taskMap = tasks.stream()
                .filter(t -> processInstanceId.equals(t.get("processInstanceId")))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Aucune tâche RH trouvée pour cette demande"));

        String taskId = (String) taskMap.get("taskId");
        workflowService.processRHDecision(taskId, true, commentaire, adminEmail);
    }

    @Override
    @Transactional
    public void refuserDemande(Long demandeId, String motif, String adminEmail) {
        DemandeConge demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée"));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Seules les demandes en attente peuvent être refusées");
        }

        String processInstanceId = demande.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new BusinessException("Demande sans instance Camunda, impossible de refuser via workflow");
        }

        List<Map<String, Object>> tasks = workflowService.getRHTasks(adminEmail);
        Map<String, Object> taskMap = tasks.stream()
                .filter(t -> processInstanceId.equals(t.get("processInstanceId")))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Aucune tâche RH trouvée pour cette demande"));

        String taskId = (String) taskMap.get("taskId");
        workflowService.processRHDecision(taskId, false, motif, adminEmail);
    }

    @Override
    public Map<String, Long> getStatsByStatut() {
        return demandeRepository.findAll().stream()
                .collect(Collectors.groupingBy(DemandeConge::getStatut, Collectors.counting()));
    }

    @Override
    public List<DemandeCongeAdminDTO> getOrphanRequests() {
        return demandeRepository.findByProcessInstanceIdIsNull().stream()
                .map(DemandeCongeAdminDTO::fromEntity)
                .collect(Collectors.toList());
    }
}