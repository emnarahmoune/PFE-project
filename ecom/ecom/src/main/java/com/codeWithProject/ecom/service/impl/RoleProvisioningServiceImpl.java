package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.RoleProvisioningService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleProvisioningServiceImpl implements RoleProvisioningService {

    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final AdministrateurRHRepository administrateurRHRepository;
    private final EntityManager entityManager;

    @Override
    public void provisionRole(Employe employe) {
        if (employe == null || employe.getId() == null) {
            return;
        }

        String role = normalizeRole(employe.getRole());
        String typeEmploye = determineTypeEmploye(role);

        entityManager.flush();

        employeRepository.updateRoleAndTypeEmploye(
                employe.getId(),
                role,
                typeEmploye
        );

        if ("MANAGER".equals(role)) {
            managerRepository.upsertManagerRow(employe.getId());
            administrateurRHRepository.deleteAdminRhRow(employe.getId());
        } else if ("ADMIN_RH".equals(role)) {
            administrateurRHRepository.insertAdminRhRow(employe.getId());
            managerRepository.deleteManagerRow(employe.getId());
        } else {
            managerRepository.deleteManagerRow(employe.getId());
            administrateurRHRepository.deleteAdminRhRow(employe.getId());
        }

        entityManager.clear();

        log.info("✅ Role/type/table synchronisés automatiquement : id={}, email={}, role={}, type_employe={}",
                employe.getId(),
                employe.getEmail(),
                role,
                typeEmploye);
    }

    @Override
    public String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "EMPLOYE";
        }

        String r = role.trim().toUpperCase();

        if ("ADMIN".equals(r)
                || "ADMIN_RH".equals(r)
                || "RH".equals(r)
                || "ADMINISTRATEUR_RH".equals(r)) {
            return "ADMIN_RH";
        }

        if ("MANAGER".equals(r)
                || "MANAGEUR".equals(r)) {
            return "MANAGER";
        }

        return "EMPLOYE";
    }

    private String determineTypeEmploye(String role) {
        if ("ADMIN_RH".equalsIgnoreCase(role)) {
            return "ADMIN_RH";
        }

        if ("MANAGER".equalsIgnoreCase(role)) {
            return "MANAGER";
        }

        return "EMPLOYE";
    }
}