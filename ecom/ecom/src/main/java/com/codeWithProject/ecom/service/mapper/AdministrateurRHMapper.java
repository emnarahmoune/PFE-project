package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import org.springframework.stereotype.Component;

@Component
public class AdministrateurRHMapper {

    public AdministrateurRHDTO toDto(AdministrateurRH entity) {
        if (entity == null) return null;

        AdministrateurRHDTO dto = new AdministrateurRHDTO();
        dto.setId(entity.getId());
        dto.setMatricule(entity.getMatricule());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setPoste(entity.getPoste());
        dto.setDepartement(entity.getDepartement());
        dto.setSalaire(
    entity.getSalaire() != null
        ? entity.getSalaire().doubleValue()
        : null
);
        dto.setStatut(entity.getStatut());
        dto.setActif(entity.getActif());
        dto.setDateEmbauche(entity.getDateEmbauche());
        dto.setDateCreation(entity.getDateCreation());
        dto.setSoldeConges(entity.getSoldeConges());
        dto.setRole(entity.getRole());
        dto.setNomComplet(entity.getNomComplet());
        dto.setStatutCompte(entity.getStatut());
        dto.setPeutSeConnecter(entity.peutSeConnecter());
        dto.setAnciennete(entity.getAnciennete());

        // Un administrateur RH n'a pas de relation 'employe' distincte
        // car il est lui-même un employé. On peut éventuellement mettre son propre ID comme employeId
        dto.setEmployeId(entity.getId());
        dto.setEmployeMatricule(entity.getMatricule());
        dto.setEmployeNom(entity.getNom());
        dto.setEmployePrenom(entity.getPrenom());

        return dto;
    }

    public AdministrateurRH toEntity(AdministrateurRHDTO dto) {
        if (dto == null) return null;

        AdministrateurRH entity = new AdministrateurRH();
        entity.setId(dto.getId());
        entity.setMatricule(dto.getMatricule());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setPoste(dto.getPoste());
        entity.setDepartement(dto.getDepartement());
        entity.setSalaire(
    dto.getSalaire() != null
        ? java.math.BigDecimal.valueOf(dto.getSalaire())
        : null
);
        entity.setStatut(dto.getStatut() != null ? dto.getStatut() : "ACTIF");
        entity.setActif(dto.getActif() != null ? dto.getActif() : true);
        entity.setDateEmbauche(dto.getDateEmbauche());
        entity.setDateCreation(dto.getDateCreation());
        entity.setSoldeConges(dto.getSoldeConges() != null ? dto.getSoldeConges() : 25);
        entity.setRole(dto.getRole() != null ? dto.getRole() : "ADMIN_RH");
        return entity;
    }
}