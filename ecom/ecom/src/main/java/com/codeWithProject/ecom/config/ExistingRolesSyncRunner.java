package com.codeWithProject.ecom.config;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.AdministrateurRHRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.repository.ManagerRepository;
import com.codeWithProject.ecom.service.RoleProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExistingRolesSyncRunner implements CommandLineRunner {

    private final EmployeRepository employeRepository;
    private final ManagerRepository managerRepository;
    private final AdministrateurRHRepository administrateurRHRepository;
    private final RoleProvisioningService roleProvisioningService;

    @Override
    @Transactional
    public void run(String... args) {

        List<Employe> employes = employeRepository.findAll();

        int managers = 0;
        int admins = 0;
        int simples = 0;

        for (Employe employe : employes) {
            if (employe.getId() == null) {
                continue;
            }

            String role = roleProvisioningService.normalizeRole(employe.getRole());

            employeRepository.updateRoleAndTypeEmploye(
                    employe.getId(),
                    role,
                    determineTypeEmploye(role)
            );

            if ("MANAGER".equals(role)) {
                managerRepository.upsertManagerRow(employe.getId());                administrateurRHRepository.deleteAdminRhRow(employe.getId());
                managers++;
            } else if ("ADMIN_RH".equals(role)) {
                administrateurRHRepository.insertAdminRhRow(employe.getId());
                managerRepository.deleteManagerRow(employe.getId());
                admins++;
            } else {
                managerRepository.deleteManagerRow(employe.getId());
                administrateurRHRepository.deleteAdminRhRow(employe.getId());
                simples++;
            }
        }

        log.info("Synchronisation anciens employés terminée : managers={}, adminsRH={}, employes={}",
                managers, admins, simples);
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