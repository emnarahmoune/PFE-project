package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.AdministrateurRH;
import com.codeWithProject.ecom.service.dto.AdministrateurRHDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper AdministrateurRH ↔ AdministrateurRHDTO
 *
 * Après refactoring : AdministrateurRH extends Utilisateur.
 * Les champs nom/prenom/email/telephone/actif/dateCreation sont des attributs
 * DIRECTS de AdministrateurRH (hérités) — plus besoin de passer par getUtilisateur().
 *
 * Anciens appels supprimés :
 *   entity.getEmploye().getUtilisateur().getNom()   → entity.getNom()
 *   dto.setNomEmploye(...)                          → dto.setNom(...)
 *   dto.setMatriculeEmploye(...)                    → dto.setMatricule(...)
 */
@Component
public class AdministrateurRHMapper {

    // ── Entité → DTO ──────────────────────────────────────────

    public AdministrateurRHDTO toDto(AdministrateurRH entity) {
        if (entity == null) return null;

        AdministrateurRHDTO dto = new AdministrateurRHDTO();
        dto.setId(entity.getId());

        // Champs hérités directement de Utilisateur
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setActif(entity.getActif());
        dto.setDateCreation(entity.getDateCreation());

        // Relation Employe (optionnelle)
        if (entity.getEmploye() != null) {
            dto.setEmployeId(entity.getEmploye().getId());
            dto.setMatricule(entity.getEmploye().getMatricule());
        }

        return dto;
    }

    // ── DTO → Entité ──────────────────────────────────────────

    public AdministrateurRH toEntity(AdministrateurRHDTO dto) {
        if (dto == null) return null;

        AdministrateurRH entity = new AdministrateurRH();
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setActif(dto.getActif() != null ? dto.getActif() : Boolean.TRUE);
        // L'employé est associé dans le service, pas ici

        return entity;
    }
}