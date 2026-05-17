package com.codeWithProject.ecom.config;

import com.codeWithProject.ecom.entity.Role;
import com.codeWithProject.ecom.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExistingRolesSyncRunner implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {

        try {

            if (roleRepository.count() == 0) {

                Role admin = new Role();
                admin.setName("ADMIN");

                Role manager = new Role();
                manager.setName("MANAGER");

                Role employee = new Role();
                employee.setName("EMPLOYEE");

                roleRepository.save(admin);
                roleRepository.save(manager);
                roleRepository.save(employee);

                log.info("Roles initialized");
            }

        } catch (Exception e) {

            log.error("Error initializing roles", e);
        }
    }
}