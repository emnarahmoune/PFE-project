package com.codeWithProject.ecom.service.mapper;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.IndicateurRH;
import com.codeWithProject.ecom.entity.SystemeBI;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.SystemeBIRepository;
import com.codeWithProject.ecom.service.dto.IndicateurRHDTO;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IndicateurRHMapper {

    private final EmployeRepository employeRepository;
    private final SystemeBIRepository systemeBIRepository;

    public IndicateurRHDTO toDto(IndicateurRH entity) {
        if (entity == null) return null;
        return IndicateurRHDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .valeur(entity.getValeur())
                .dateCalcul(entity.getDateCalcul())
                .periode(entity.getPeriode())
                .annee(entity.getAnnee())
                .mois(entity.getMois())
                .trimestre(entity.getTrimestre())
                .departement(entity.getDepartement())
                .commentaire(entity.getCommentaire())
                .tendance(entity.getTendance())
                .valeurPrecedente(entity.getValeurPrecedente())
                .employeId(entity.getEmploye() != null ? entity.getEmploye().getId() : null)
                .systemeBIId(entity.getSystemeBI() != null ? entity.getSystemeBI().getId() : null)
                .build();
    }

    public IndicateurRH toEntity(IndicateurRHDTO dto) {
        if (dto == null) return null;
        IndicateurRH.IndicateurRHBuilder builder = IndicateurRH.builder()
                .id(dto.getId())
                .type(dto.getType())
                .valeur(dto.getValeur())
                .dateCalcul(dto.getDateCalcul())
                .periode(dto.getPeriode())
                .annee(dto.getAnnee())
                .mois(dto.getMois())
                .trimestre(dto.getTrimestre())
                .departement(dto.getDepartement())
                .commentaire(dto.getCommentaire())
                .tendance(dto.getTendance())
                .valeurPrecedente(dto.getValeurPrecedente());

        if (dto.getEmployeId() != null) {
            Employe employe = employeRepository.findById(dto.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employe", dto.getEmployeId()));
            builder.employe(employe);
        }
        if (dto.getSystemeBIId() != null) {
            SystemeBI systemeBI = systemeBIRepository.findById(dto.getSystemeBIId())
                    .orElseThrow(() -> new ResourceNotFoundException("SystemeBI", dto.getSystemeBIId()));
            builder.systemeBI(systemeBI);
        }
        return builder.build();
    }
}