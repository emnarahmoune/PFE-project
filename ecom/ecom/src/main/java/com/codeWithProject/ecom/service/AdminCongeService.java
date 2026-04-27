package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import java.util.List;
import java.util.Map;

public interface AdminCongeService {

    List<DemandeCongeAdminDTO> getDemandesEnAttentePlusDe10Jours();

    List<DemandeCongeAdminDTO> getAllDemandes();

    DemandeCongeAdminDTO getDemandeById(Long id);

    void validerDemande(Long demandeId, String commentaire, String adminEmail);

    void refuserDemande(Long demandeId, String motif, String adminEmail);

    Map<String, Long> getStatsByStatut();


    // ✅ Méthode manquante ajoutée
    List<DemandeCongeAdminDTO> getOrphanRequests();

    List<DemandeCongeAdminDTO> getDemandesRefuseesParManager();
}