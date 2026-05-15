package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Manager;
import com.codeWithProject.ecom.service.dto.ManagerDTO;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class ManagerMapper {

    public ManagerDTO toDto(Manager entity) {
        if (entity == null) return null;

        ManagerDTO dto = new ManagerDTO();
        dto.setId(entity.getId());
        dto.setMatricule(entity.getMatricule());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setPoste(entity.getPoste());
        dto.setSalaire(
    entity.getSalaire() != null
        ? entity.getSalaire().doubleValue()
        : null
);
        dto.setStatut(entity.getStatut());
        dto.setDepartement(entity.getDepartement());
        dto.setSoldeConges(entity.getSoldeConges());
        dto.setActif(entity.getActif());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateEmbauche(entity.getDateEmbauche());   // ✅ maintenant disponible
        dto.setRole(entity.getRole());

        dto.setDateNomination(entity.getDateNomination());

        dto.setEmployeId(entity.getId());
        dto.setEmployeMatricule(entity.getMatricule());
        dto.setEmployeNom(entity.getNom());
        dto.setEmployePrenom(entity.getPrenom());

        if (entity.getEmployesGeres() != null) {
            dto.setNombreEmployesGeres(entity.getNombreEmployesGeres());
            dto.setNombreEmployesTotal(entity.getNombreEmployesTotal());
            dto.setEmployesGeresIds(entity.getEmployesGeres().stream()
                    .map(emp -> emp.getId())
                    .collect(Collectors.toList()));
        }

        if (entity.getDemandesCongeAValider() != null) {
            dto.setNombreDemandesEnAttente(entity.getNombreDemandesEnAttente());
            dto.setDemandesEnAttenteIds(entity.getDemandesCongeAValider().stream()
                    .filter(d -> "EN_ATTENTE".equals(d.getStatut()))
                    .map(d -> d.getId())
                    .collect(Collectors.toList()));

            var urgentes = entity.getDemandesUrgentes();
            dto.setNombreDemandesUrgentes(urgentes.size());
            dto.setADesDemandesUrgentes(!urgentes.isEmpty());
        }

        dto.setNomComplet(entity.getNomComplet());
        dto.setAnciennete(entity.getAnciennete());
        dto.setAncienneteManager(entity.getAncienneteManager());
        dto.setStatutCompte(entity.getStatut());
        dto.setPeutSeConnecter(entity.peutSeConnecter());

        return dto;
    }

    public Manager toEntity(ManagerDTO dto) {
        if (dto == null) return null;

        Manager entity = new Manager();
        entity.setId(dto.getId());
        entity.setMatricule(dto.getMatricule());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setDateEmbauche(dto.getDateEmbauche());
        entity.setPoste(dto.getPoste());
        entity.setSalaire(
    dto.getSalaire() != null
        ? java.math.BigDecimal.valueOf(dto.getSalaire())
        : null
);
        entity.setStatut(dto.getStatut() != null ? dto.getStatut() : "ACTIF");
        entity.setDepartement(dto.getDepartement());
        entity.setSoldeConges(dto.getSoldeConges() != null ? dto.getSoldeConges() : 25);
        entity.setActif(dto.getActif() != null ? dto.getActif() : true);
        entity.setRole(dto.getRole() != null ? dto.getRole() : "manager");
        entity.setDateNomination(dto.getDateNomination());
        return entity;
    }
}