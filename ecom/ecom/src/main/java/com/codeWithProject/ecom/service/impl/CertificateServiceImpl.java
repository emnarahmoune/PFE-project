package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.*;
import com.codeWithProject.ecom.repository.*;
import com.codeWithProject.ecom.service.CertificateService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final EmployeRepository employeRepository;
    private final FormationRepository formationRepository;
    private final EmployeFormationRepository employeFormationRepository;

    @Override
    public Certificate generateCertificate(Long employeId, Long formationId) {

        return certificateRepository
                .findByEmploye_IdAndFormation_Id(employeId, formationId)
                .orElseGet(() -> {

                    Employe employe = employeRepository.findById(employeId)
                            .orElseThrow(() -> new RuntimeException("Employé introuvable"));

                    Formation formation = formationRepository.findById(formationId)
                            .orElseThrow(() -> new RuntimeException("Formation introuvable"));

                    EmployeFormation ef = employeFormationRepository
                            .findByEmploye_IdAndFormation_Id(employeId, formationId)
                            .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

                    if (ef.getProgression() < 100) {
                        throw new RuntimeException("Formation non terminée");
                    }

                    Certificate certificate = Certificate.builder()
                            .employe(employe)
                            .formation(formation)
                            .dateGeneration(LocalDateTime.now())
                            .certificateCode("CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .build();

                    return certificateRepository.save(certificate);
                });
    }

    @Override
    public List<Certificate> getMyCertificates(Long employeId) {
        return certificateRepository.findByEmploye_Id(employeId);
    }
}