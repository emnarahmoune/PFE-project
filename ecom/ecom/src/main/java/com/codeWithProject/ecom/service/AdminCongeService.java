package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import java.util.List;
import java.util.Map;

public interface AdminCongeService {

    /**
     * Récupère toutes les demandes de congé en attente avec plus de 10 jours
     */
    List<DemandeCongeAdminDTO> getDemandesEnAttentePlusDe10Jours();

    /**
     * Récupère toutes les demandes de congé (pour admin)
     */
    List<DemandeCongeAdminDTO> getAllDemandes();

    /**
     * Récupère une demande par son ID
     */
    DemandeCongeAdminDTO getDemandeById(Long id);

    /**
     * Approuve une demande de congé
     */
    void validerDemande(Long demandeId, String commentaire);

    /**
     * Refuse une demande de congé avec un motif
     */
    void refuserDemande(Long demandeId, String motif);

    /**
     * Statistiques des demandes par statut
     */
    Map<String, Long> getStatsByStatut();

    /**
     * Récupère les demandes orphelines (sans instance Camunda)
     */
    List<DemandeCongeAdminDTO> getOrphanRequests();
}