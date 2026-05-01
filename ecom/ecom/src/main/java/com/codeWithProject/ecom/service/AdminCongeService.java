package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.DemandeCongeAdminDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusDetailsDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusManagerDTO;

import java.util.List;
import java.util.Map;

public interface AdminCongeService {

    List<DemandeCongeAdminDTO> getDemandesEnAttentePlusDe10Jours();

    List<DemandeCongeAdminDTO> getAllDemandes();

    DemandeCongeAdminDTO getDemandeById(Long id);

    void validerDemande(Long demandeId, String commentaire, String adminEmail);

    void refuserDemande(Long demandeId, String motif, String adminEmail);

    Map<String, Long> getStatsByStatut();

    List<DemandeRefusManagerDTO> getDemandesRefuseesParManager();
    // ✅ Méthode manquante ajoutée
    List<DemandeCongeAdminDTO> getOrphanRequests();
    // NOUVELLES MÉTHODES
    List<CalendarEventDTO> getAllCalendarEvents();
    DemandeRefusDetailsDTO getRefusDetails(Long id);

}